package com.candor.girder.lexer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.candor.girder.lexer.Token.Type;
import org.json.JSONObject;

public class Lexer {
    public List<String> operators;
    private JSONObject bindings;
    private String lineSeparator;
    private String forbiddenCharacters;
    private Character space;

    @SuppressWarnings("SpellCheckingInspection")
    private enum Symbol {
        SPACE,
        LINESEP,
        VALSEP,
        DEFOPEN,
        DEFCLOSE,
        CALLOPEN,
        CALLCLOSE,
        STROPEN,
        STRCLOSE
    }

    private Map<Symbol, String> special_character_config_names;

    public List<Token> lexInstruction(String instruction, int line, String filePath) throws LexerException {
        List<Token> tokens = new ArrayList<>();
        instruction.replace(System.lineSeparator(), "");

        String previousId = "";
        String currentId = "";

        String name = "";

        int callLevel = 0;
        int bracketLevel = 0;
        int bracketContains = 0;
        int callContains = 0;
        boolean countingBracketLevel = false;
        boolean countingCallLevel = false;
        int bracketOpenIndex = 0;

        boolean bind = false;
        Type type = null;
        for (int i = 0; i < instruction.length(); i++) {
            previousId = currentId;
            Character c = instruction.charAt(i);
            currentId += c;

            if (bindingStartsWith(currentId)) {
                if (isABinding(currentId)) {
                    switch (recognizeBinding(currentId)) {
                        case VALSEP:
                            if (countingBracketLevel) {
                                bracketLevel++;
                            } else if (countingCallLevel) {
                                bracketLevel++;
                            } else {
                                throw new SyntaxError("Misplaced value separator (\"" + "\"). Check your syntax.", line, i, filePath);
                            }
                            break;

                        case LINESEP:
                            if (countingBracketLevel || countingCallLevel) {
                                bracketLevel++;
                            } else {
                                tokens.add(new Token(Type.LINE_SEPARATOR, binding(Symbol.LINESEP)));
                            } break;

                        case DEFOPEN:
                            if (!countingBracketLevel && !countingCallLevel) {
                                countingBracketLevel = true;
                                bracketLevel = 0;
                            }
                            else {
                                bracketLevel++;
                            } break;

                        case DEFCLOSE:
                            if (bracketLevel == 0) {
                                countingBracketLevel = false;
                                i = bracketOpenIndex;
                                tokens.add(new Token(Type.DEFINITION, name, callLevel, bracketContains));
                            } else {
                                bracketLevel--;
                            } break;

                        case CALLOPEN:
                            if (countingCallLevel || countingBracketLevel) {
                                bracketLevel++;
                            } else {
                                countingCallLevel = true;
                                bracketLevel = 0;
                                bracketOpenIndex = i;
                            }
                        case CALLCLOSE:
                            if (countingCallLevel) {
                                if (bracketLevel == 0) {
                                    bracketLevel--;
                                } else {
                                    countingCallLevel = false;
                                    callContains = bracketContains;
                                }
                            } else if (countingBracketLevel) {
                                if (bracketLevel == 0) {
                                    throw new SyntaxError("A definition bracket (\"" + binding(Symbol.DEFOPEN)
                                            + "\") has been closed with a call bracket (\"" + binding(Symbol.CALLCLOSE)
                                            + "\") whereas it should have been closed with a definition bracket (duh) (\""
                                            + binding(Symbol.DEFCLOSE) + "\"). Check your brackets.", line, i, filePath);
                                } else {
                                    bracketLevel--;
                                }
                            } break;
                    }
                } else {
                    continue;
                }
            }

            if (operatorStartsWith(currentId)) {
                type = Type.OPERATOR;
            } else if (type == Type.OPERATOR) {
                tokens.add(new Token(Type.OPERATOR, previousId));
                currentId = "" + c;
                type = null;
                continue;
            }
        }
        return tokens;
    }

    private boolean isABinding(String s) {
        return bindings.has(s);
    }

    private boolean bindingStartsWith(String str) {
        for (Iterator<String> i = bindings.keys(); i.hasNext(); i.next()) {
            if (bindings.getString(i.toString()).startsWith(str)) {
                return true;
            }
        }
        return false;
    }

    private Symbol recognizeBinding(String s) {
        for (Object i : bindings.keySet()) {
            if (s.equals(bindings.getString(special_character_config_names.get(i)))) {
                return (Symbol) i;
            }
        }
        return null;
    }

    private String binding(Symbol c) {
        return bindings.getString(special_character_config_names.get(c));
    }

    private boolean operatorStartsWith(String str) {
        for (String s : operators) {
            if (s.startsWith(str)) {
                return true;
            }
        }
        return false;
    }

    private void init() {
        lineSeparator = ";";
        special_character_config_names = Map.of(
                Symbol.SPACE, "space",
                Symbol.LINESEP, "lineSeparator",
                Symbol.VALSEP, "valueSeparator",
                Symbol.DEFOPEN, "definitionOpen",
                Symbol.DEFCLOSE, "definitionClose",
                Symbol.CALLOPEN, "callOpen",
                Symbol.CALLCLOSE, "callClose",
                Symbol.STROPEN, "stringOpen",
                Symbol.STRCLOSE, "stringClose"
        );
    }

    public Lexer() {
        init();
    }

    public Lexer(String configFile) {
        init();
    }
}
