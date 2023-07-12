package com.candor.girder.lexer;

public class Token {
    public int range;
    public int secondaryRange;
    public Type type;
    public String value;

    public enum Type {
        CALL,
        DEFINITION,
        OPERATOR,
        VALUE,
        VARIABLE,
        LINE_SEPARATOR
    }

    public Token(Type _type, String _value, int _range, int _secondaryRange) {
        type = _type;
        value = _value;
        range = _range;
        secondaryRange = _secondaryRange;
    }

    public Token(Type _type, String _value, int _range) {
        this(_type, _value, _range, 0);
    }

    public Token(Type _type, String _value) {
        this(_type, _value, 0);
    }
}
