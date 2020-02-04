package io.opentelemetry.auto.instrumentation.hibernate.core.v3_3;

import static io.opentelemetry.auto.instrumentation.hibernate.HibernateDecorator.DECORATOR;
import static io.opentelemetry.auto.tooling.ByteBuddyElementMatchers.safeHasSuperType;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;

import com.google.auto.service.AutoService;
import io.opentelemetry.auto.bootstrap.ContextStore;
import io.opentelemetry.auto.bootstrap.InstrumentationContext;
import io.opentelemetry.auto.instrumentation.api.SpanScopePair;
import io.opentelemetry.auto.instrumentation.hibernate.SessionMethodUtils;
import io.opentelemetry.auto.tooling.Instrumenter;
import io.opentelemetry.trace.Span;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;
import org.hibernate.Query;
import org.hibernate.SQLQuery;

@AutoService(Instrumenter.class)
public class QueryInstrumentation extends AbstractHibernateInstrumentation {

  @Override
  public Map<String, String> contextStore() {
    return singletonMap("org.hibernate.Query", Span.class.getName());
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return not(isInterface()).and(safeHasSuperType(named("org.hibernate.Query")));
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return singletonMap(
        isMethod()
            .and(
                named("list")
                    .or(named("executeUpdate"))
                    .or(named("uniqueResult"))
                    .or(named("scroll"))),
        QueryInstrumentation.class.getName() + "$QueryMethodAdvice");
  }

  public static class QueryMethodAdvice extends V3Advice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static SpanScopePair startMethod(
        @Advice.This final Query query, @Advice.Origin("#m") final String name) {

      final ContextStore<Query, Span> contextStore =
          InstrumentationContext.get(Query.class, Span.class);

      // Note: We don't know what the entity is until the method is returning.
      final SpanScopePair spanScopePair =
          SessionMethodUtils.startScopeFrom(
              contextStore, query, "hibernate.query." + name, null, true);
      if (spanScopePair != null) {
        DECORATOR.onStatement(spanScopePair.getSpan(), query.getQueryString());
      }
      return spanScopePair;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void endMethod(
        @Advice.This final Query query,
        @Advice.Enter final SpanScopePair spanScopePair,
        @Advice.Thrown final Throwable throwable,
        @Advice.Return(typing = Assigner.Typing.DYNAMIC) final Object returned) {

      Object entity = returned;
      if (returned == null || query instanceof SQLQuery) {
        // Not a method that returns results, or the query returns a table rather than an ORM
        // object.
        entity = query.getQueryString();
      }

      SessionMethodUtils.closeScope(spanScopePair, throwable, entity);
    }
  }
}
