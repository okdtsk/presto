/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.prestosql.sql.planner.iterative.rule;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.prestosql.sql.planner.Symbol;
import io.prestosql.sql.planner.assertions.ExpressionMatcher;
import io.prestosql.sql.planner.iterative.rule.test.BaseRuleTest;
import io.prestosql.sql.planner.plan.JoinNode;
import io.prestosql.sql.tree.ComparisonExpression;
import io.prestosql.sql.tree.LongLiteral;
import org.testng.annotations.Test;

import java.util.Optional;

import static io.prestosql.sql.planner.assertions.PlanMatchPattern.filter;
import static io.prestosql.sql.planner.assertions.PlanMatchPattern.join;
import static io.prestosql.sql.planner.assertions.PlanMatchPattern.limit;
import static io.prestosql.sql.planner.assertions.PlanMatchPattern.project;
import static io.prestosql.sql.planner.assertions.PlanMatchPattern.values;
import static io.prestosql.sql.planner.plan.CorrelatedJoinNode.Type.FULL;
import static io.prestosql.sql.planner.plan.CorrelatedJoinNode.Type.INNER;
import static io.prestosql.sql.planner.plan.CorrelatedJoinNode.Type.LEFT;
import static io.prestosql.sql.planner.plan.CorrelatedJoinNode.Type.RIGHT;
import static io.prestosql.sql.tree.BooleanLiteral.TRUE_LITERAL;
import static io.prestosql.sql.tree.ComparisonExpression.Operator.GREATER_THAN;
import static io.prestosql.sql.tree.ComparisonExpression.Operator.LESS_THAN;

public class TestTransformCorrelatedJoinToJoin
        extends BaseRuleTest
{
    @Test
    public void testRewriteInnerCorrelatedJoin()
    {
        tester().assertThat(new TransformCorrelatedJoinToJoin(tester().getMetadata()))
                .on(p -> {
                    Symbol a = p.symbol("a");
                    Symbol b = p.symbol("b");
                    return p.correlatedJoin(
                            ImmutableList.of(a),
                            p.values(a),
                            p.filter(
                                    new ComparisonExpression(
                                            GREATER_THAN,
                                            b.toSymbolReference(),
                                            a.toSymbolReference()),
                                    p.values(b)));
                })
                .matches(
                        join(
                                JoinNode.Type.INNER,
                                ImmutableList.of(),
                                Optional.of("b > a"),
                                values("a"),
                                filter(
                                        TRUE_LITERAL,
                                        values("b"))));

        tester().assertThat(new TransformCorrelatedJoinToJoin(tester().getMetadata()))
                .on(p -> {
                    Symbol a = p.symbol("a");
                    Symbol b = p.symbol("b");
                    return p.correlatedJoin(
                            ImmutableList.of(a),
                            p.values(a),
                            INNER,
                            new ComparisonExpression(
                                    LESS_THAN,
                                    b.toSymbolReference(),
                                    new LongLiteral("3")),
                            p.filter(
                                    new ComparisonExpression(
                                            GREATER_THAN,
                                            b.toSymbolReference(),
                                            a.toSymbolReference()),
                                    p.values(b)));
                })
                .matches(
                        join(
                                JoinNode.Type.INNER,
                                ImmutableList.of(),
                                Optional.of("b > a AND b < 3"),
                                values("a"),
                                filter(
                                        TRUE_LITERAL,
                                        values("b"))));
    }

    @Test
    public void testRewriteLeftCorrelatedJoin()
    {
        tester().assertThat(new TransformCorrelatedJoinToJoin(tester().getMetadata()))
                .on(p -> {
                    Symbol a = p.symbol("a");
                    Symbol b = p.symbol("b");
                    return p.correlatedJoin(
                            ImmutableList.of(a),
                            p.values(a),
                            LEFT,
                            TRUE_LITERAL,
                            p.filter(
                                    new ComparisonExpression(
                                            GREATER_THAN,
                                            b.toSymbolReference(),
                                            a.toSymbolReference()),
                                    p.values(b)));
                })
                .matches(
                        join(
                                JoinNode.Type.LEFT,
                                ImmutableList.of(),
                                Optional.of("b > a"),
                                values("a"),
                                filter(
                                        TRUE_LITERAL,
                                        values("b"))));

        tester().assertThat(new TransformCorrelatedJoinToJoin(tester().getMetadata()))
                .on(p -> {
                    Symbol a = p.symbol("a");
                    Symbol b = p.symbol("b");
                    return p.correlatedJoin(
                            ImmutableList.of(a),
                            p.values(a),
                            LEFT,
                            new ComparisonExpression(
                                    LESS_THAN,
                                    b.toSymbolReference(),
                                    new LongLiteral("3")),
                            p.filter(
                                    new ComparisonExpression(
                                            GREATER_THAN,
                                            b.toSymbolReference(),
                                            a.toSymbolReference()),
                                    p.values(b)));
                })
                .matches(
                        join(
                                JoinNode.Type.LEFT,
                                ImmutableList.of(),
                                Optional.of("b > a AND b < 3"),
                                values("a"),
                                filter(
                                        TRUE_LITERAL,
                                        values("b"))));
    }

    @Test
    public void testRewriteRightCorrelatedJoin()
    {
        tester().assertThat(new TransformCorrelatedJoinToJoin(tester().getMetadata()))
                .on(p -> {
                    Symbol a = p.symbol("a");
                    Symbol b = p.symbol("b");
                    return p.correlatedJoin(
                            ImmutableList.of(a),
                            p.values(a),
                            RIGHT,
                            TRUE_LITERAL,
                            p.filter(
                                    new ComparisonExpression(
                                            GREATER_THAN,
                                            b.toSymbolReference(),
                                            a.toSymbolReference()),
                                    p.values(b)));
                })
                .matches(
                        join(
                                JoinNode.Type.INNER,
                                ImmutableList.of(),
                                Optional.of("b > a"),
                                values("a"),
                                filter(
                                        TRUE_LITERAL,
                                        values("b"))));

        tester().assertThat(new TransformCorrelatedJoinToJoin(tester().getMetadata()))
                .on(p -> {
                    Symbol a = p.symbol("a");
                    Symbol b = p.symbol("b");
                    return p.correlatedJoin(
                            ImmutableList.of(a),
                            p.values(a),
                            RIGHT,
                            new ComparisonExpression(
                                    LESS_THAN,
                                    b.toSymbolReference(),
                                    new LongLiteral("3")),
                            p.topN(
                                    2,
                                    ImmutableList.of(a),
                                    p.values(b)));
                })
                .matches(
                        project(
                                ImmutableMap.of(
                                        "a", new ExpressionMatcher("if(b < 3, a, null)"),
                                        "b", new ExpressionMatcher("b")),
                                join(
                                        JoinNode.Type.INNER,
                                        ImmutableList.of(),
                                        Optional.empty(),
                                        values("a"),
                                        limit(
                                                2,
                                                values("b")))));

        tester().assertThat(new TransformCorrelatedJoinToJoin(tester().getMetadata()))
                .on(p -> {
                    Symbol a = p.symbol("a");
                    Symbol b = p.symbol("b");
                    return p.correlatedJoin(
                            ImmutableList.of(a),
                            p.values(a),
                            RIGHT,
                            new ComparisonExpression(
                                    LESS_THAN,
                                    b.toSymbolReference(),
                                    new LongLiteral("3")),
                            p.filter(
                                    new ComparisonExpression(
                                            GREATER_THAN,
                                            b.toSymbolReference(),
                                            a.toSymbolReference()),
                                    p.values(b)));
                })
                .matches(
                        project(
                                ImmutableMap.of(
                                        "a", new ExpressionMatcher("if(b < 3, a, null)"),
                                        "b", new ExpressionMatcher("b")),
                                join(
                                        JoinNode.Type.INNER,
                                        ImmutableList.of(),
                                        Optional.of("b > a"),
                                        values("a"),
                                        filter(
                                                TRUE_LITERAL,
                                                values("b")))));
    }

    @Test
    public void testRewriteFullCorrelatedJoin()
    {
        tester().assertThat(new TransformCorrelatedJoinToJoin(tester().getMetadata()))
                .on(p -> {
                    Symbol a = p.symbol("a");
                    Symbol b = p.symbol("b");
                    return p.correlatedJoin(
                            ImmutableList.of(a),
                            p.values(a),
                            FULL,
                            TRUE_LITERAL,
                            p.filter(
                                    new ComparisonExpression(
                                            GREATER_THAN,
                                            b.toSymbolReference(),
                                            a.toSymbolReference()),
                                    p.values(b)));
                })
                .matches(
                        join(
                                JoinNode.Type.LEFT,
                                ImmutableList.of(),
                                Optional.of("b > a"),
                                values("a"),
                                filter(
                                        TRUE_LITERAL,
                                        values("b"))));

        tester().assertThat(new TransformCorrelatedJoinToJoin(tester().getMetadata()))
                .on(p -> {
                    Symbol a = p.symbol("a");
                    Symbol b = p.symbol("b");
                    return p.correlatedJoin(
                            ImmutableList.of(a),
                            p.values(a),
                            FULL,
                            new ComparisonExpression(
                                    LESS_THAN,
                                    b.toSymbolReference(),
                                    new LongLiteral("3")),
                            p.topN(
                                    2,
                                    ImmutableList.of(a),
                                    p.values(b)));
                })
                .doesNotFire();

        tester().assertThat(new TransformCorrelatedJoinToJoin(tester().getMetadata()))
                .on(p -> {
                    Symbol a = p.symbol("a");
                    Symbol b = p.symbol("b");
                    return p.correlatedJoin(
                            ImmutableList.of(a),
                            p.values(a),
                            FULL,
                            new ComparisonExpression(
                                    LESS_THAN,
                                    b.toSymbolReference(),
                                    new LongLiteral("3")),
                            p.filter(
                                    new ComparisonExpression(
                                            GREATER_THAN,
                                            b.toSymbolReference(),
                                            a.toSymbolReference()),
                                    p.values(b)));
                })
                .doesNotFire();
    }

    @Test
    public void doesNotFireOnUncorrelated()
    {
        tester().assertThat(new TransformCorrelatedJoinToJoin(tester().getMetadata()))
                .on(p -> p.correlatedJoin(
                        ImmutableList.of(),
                        p.values(p.symbol("a")),
                        p.values(p.symbol("b"))))
                .doesNotFire();
    }
}
