package org.jtwig.highlight.parser;

import org.jtwig.highlight.format.Formatter;
import org.jtwig.highlight.parser.base.BasicParser;
import org.jtwig.highlight.parser.context.ParserContext;
import org.jtwig.highlight.parser.operators.Operator;
import org.parboiled.Rule;
import org.parboiled.annotations.Label;

import java.util.List;

public class ExpressionParser extends BasicParser {
    public ExpressionParser(ParserContext context) {
        super(ExpressionParser.class, context);
    }

    @Override
    @Label("Expression")
    protected Rule parse() {
        return FirstOf(
                Ternary(),
                Binary(),
                Unary(),
                AccessExpression(),
                Primary()
        );
    }

    @Label("Access")
    Rule AccessExpression() {
        SpacingParser spacingParser = getParserContext().parsers().get(SpacingParser.class);
        return Sequence(
                Primary(),
                spacingParser.parse(),
                String("["), push(getParserContext().formatter().operator(match())),
                spacingParser.parse(),
                parse(),
                spacingParser.parse(),
                String("]"), push(getParserContext().formatter().operator(match())),

                push(mergeSince(6))
        );
    }

    @Label("Ternary")
    Rule Ternary() {
        SpacingParser spacingParser = getParserContext().parsers().get(SpacingParser.class);
        return Sequence(
                Primary(),
                spacingParser.parse(),
                String("?"), push(getParserContext().formatter().operator(match())),
                spacingParser.parse(),
                parse(),
                spacingParser.parse(),
                String(":"), push(getParserContext().formatter().operator(match())),
                spacingParser.parse(),
                parse(),
                push(mergeSince(8))
        );
    }

    @Label("Binary")
    Rule Binary() {
        SpacingParser spacingParser = getParserContext().parsers().get(SpacingParser.class);
        return Sequence(
                Primary(),
                spacingParser.parse(),
                AnyOperator(getParserContext().binaryOperators()),
                spacingParser.parse(),
                parse(),
                push(mergeSince(4))
        );
    }

    @Label("Unary")
    Rule Unary() {
        SpacingParser spacingParser = getParserContext().parsers().get(SpacingParser.class);

        return Sequence(
                AnyOperator(getParserContext().unaryOperators()),
                spacingParser.parse(),
                Primary(),
                push(mergeSince(2))
        );
    }

    Rule AnyOperator(List<Operator> operators) {
        Rule[] rules = new Rule[operators.size()];
        for (int i = 0; i < operators.size(); i++) {
            rules[i] = Operator(operators.get(i));
        }
        return Sequence(
                FirstOf(rules),
                push(getParserContext().formatter().operator(match()))
        );
    }

    Rule Operator(Operator operator) {
        if (operator.isIdentifierPattern()) {
            return Sequence(
                    operatorSymbol(operator),
                    Test(AnyOf(" \n\t"))
            );
        } else {
            return operatorSymbol(operator);
        }
    }

    @Label("Operator Symbol")
    Rule operatorSymbol(Operator operator) {
        return String(operator.getSymbol());
    }

    @Label("Primary")
    Rule Primary() {
        return FirstOf(
                Literals(),
                List(),
                Map(),
                MethodCall(),
                Identifier(),
                ParentsisExpression()
        );
    }

    @Label("Literals")
    Rule Literals() {
        return FirstOf(
                StringExpression(),
                NumberExpression(),
                BooleanExpression()
        );
    }

    @Label("List")
    Rule List() {
        SpacingParser spacingParser = getParserContext().parsers().get(SpacingParser.class);
        return Sequence(
                String("["), push(getParserContext().formatter().startList()),
                spacingParser.parse(),
                Arguments(),
                spacingParser.parse(),
                String("]"), push(getParserContext().formatter().endList()),

                push(mergeSince(4))
        );
    }

    @Label("Map")
    Rule Map() {
        SpacingParser spacingParser = getParserContext().parsers().get(SpacingParser.class);
        return Sequence(
                String("{"), push(getParserContext().formatter().startMap()),
                spacingParser.parse(),
                MapDefinitions(),
                spacingParser.parse(),
                String("}"), push(getParserContext().formatter().endMap()),

                push(mergeSince(4))
        );
    }

    @Label("Map definitions")
    Rule MapDefinitions() {
        SpacingParser spacingParser = getParserContext().parsers().get(SpacingParser.class);
        return FirstOf(
                Sequence(
                        Sequence(
                                FirstOf(
                                        StringExpression(),
                                        Identifier()
                                ),
                                spacingParser.parse(),
                                String(":"), push(getParserContext().formatter().operator(":")),
                                spacingParser.parse(),
                                parse(),
                                spacingParser.parse(),

                                push(mergeSince(5))
                        ),
                        FirstOf(
                                OneOrMore(
                                        Sequence(
                                                Sequence(
                                                        String(","), push(getParserContext().formatter().operator(",")),
                                                        spacingParser.parse(),
                                                        FirstOf(
                                                                StringExpression(),
                                                                Identifier()
                                                        ),
                                                        spacingParser.parse(),
                                                        String(":"), push(getParserContext().formatter().operator(":")),
                                                        spacingParser.parse(),
                                                        parse(),
                                                        spacingParser.parse()
                                                ),
                                                push(mergeSince(8))
                                        )
                                ),
                                push(pop())
                        )
                ),
                spacingParser.parse()
        );
    }

    @Label("Boolean")
    Rule BooleanExpression() {
        return Sequence(
                FirstOf(
                        "true",
                        "false"
                ),
                push(getParserContext().formatter().booleanLiteral(match()))
        );
    }

    @Label("Number")
    Rule NumberExpression() {
        return Sequence(
                Sequence(
                        Optional(
                                ZeroOrMore(CharRange('0', '9')),
                                String("."),
                                Test(CharRange('0', '9'))
                        ),
                        OneOrMore(CharRange('0', '9'))
                ),
                push(getParserContext().formatter().numberLiteral(match()))
        );
    }

    @Label("String")
    Rule StringExpression() {
        return Sequence(
                FirstOf(
                        StringWith("'"),
                        StringWith("\"")
                ),
                push(getParserContext().formatter().stringLiteral(match()))
        );
    }

    Rule StringWith(String symbol) {
        return Sequence(
                String(symbol),
                ZeroOrMore(
                        Sequence(
                                TestNot(String(symbol)),
                                ANY
                        )
                ),
                String(symbol)
        );
    }

    @Label("Parentsis")
    Rule ParentsisExpression() {
        SpacingParser spacingParser = getParserContext().parsers().get(SpacingParser.class);
        Formatter formatter = getParserContext().formatter();
        return Sequence(
                String("("), push(formatter.startParentsis()),
                spacingParser.parse(),
                parse(),
                String(")"), push(formatter.endParentsis()),
                push(mergeSince(3))
        );
    }

    @Label("Identifier")
    public Rule Identifier() {
        IdentifierParser identifierParser = getParserContext().parsers().get(IdentifierParser.class);
        return Sequence(
                identifierParser.parse(),
                push(getParserContext().formatter().variable(match()))
        );
    }

    @Label("MethodCall")
    public Rule MethodCall() {
        Formatter formatter = getParserContext().formatter();
        IdentifierParser identifierParser = getParserContext().parsers().get(IdentifierParser.class);
        SpacingParser spacingParser = getParserContext().parsers().get(SpacingParser.class);
        return Sequence(
                identifierParser.parse(), push(getParserContext().formatter().variable(match())),
                spacingParser.parse(),
                String("("), push(formatter.startParentsis()),
                spacingParser.parse(),
                Arguments(),
                spacingParser.parse(),
                String(")"), push(formatter.endParentsis()),

                push(mergeSince(6))
        );
    }

    @Label("Arguments")
    Rule Arguments() {
        SpacingParser spacingParser = getParserContext().parsers().get(SpacingParser.class);
        return FirstOf(
                Sequence(
                        parse(),
                        FirstOf(
                                OneOrMore(
                                        Sequence(
                                                spacingParser.parse(),
                                                String(","), push(getParserContext().formatter().operator(",")),
                                                spacingParser.parse(),
                                                parse(),

                                                push(mergeSince(4))
                                        )
                                ),
                                push(mergeSince(0))
                        )
                ),
                push("")
        );
    }
}
