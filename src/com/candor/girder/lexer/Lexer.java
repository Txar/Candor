package com.candor.girder.lexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.candor.girder.file.FileLoader;
import com.candor.girder.lexer.Token.Type;
import org.json.JSONArray;
import org.json.JSONObject;

public class Lexer {
    public List<String> operators;
    private JSONObject config;
    private Map<String, String> bindings;
    private String lineSeparator;
    private String forbiddenCharacters;
    private Character DISABLE_NEXT_CHAR;
    private final String config_path;

    @SuppressWarnings("SpellCheckingInspection")
    private enum Symbol {
        SPACE,
        LINESEP,
        VALSEP,
        DEFOPEN,
        DEFCLOSE,
        CALLOPEN,
        CALLCLOSE,
        STRBRACKET,
        NONE
    }

    private Map<Symbol, String> special_character_config_names;

    public List<Token> lexInstruction(String instruction, int line, String filePath) throws LexerException {
        List<Token> tokens = new ArrayList<>();
        instruction.replace(System.lineSeparator(), "");

        String previousId = "";
        String currentId = "";

        String name = "";
        boolean nameOver = false;

        boolean disableNextChar = false;

        int previousTokenCount = 0;

        String callName = "";
        int callLevel = 0;
        int bracketLevel = 0;
        int bracketContains = 0;
        int callContains = 0;
        boolean countingBracketLevel = false;
        boolean countingCallLevel = false;
        int bracketOpenIndex = 0;

        boolean isAString = false;
        boolean isACall = false;
        boolean isAValue = false;

        boolean bind = false;
        Type type = null;
        for (int i = 0; i <= instruction.length(); i++) {
            previousId = currentId;
            Character c;
            if (i == instruction.length()) c = null;
            else c = instruction.charAt(i);
            currentId += c;

            if ((c + "").equals(binding(Symbol.SPACE))) {
                if (!isAString) {
                    if (isACall) {
                        isACall = false;
                        tokens.add(new Token(Type.CALL, callName, callContains));
                        previousTokenCount = tokens.size();
                        callContains = 0;
                    }
                    currentId = "";
                    nameOver = true;
                }
            }

            if (bindingStartsWith(c + "")) {
                currentId = c + "";
            }

            if (bindingStartsWith(currentId)) {
                if (isABinding(currentId)) {
                    switch (recognizeBinding(currentId)) {
                        case STRBRACKET:
                            if (isAString && !disableNextChar) {
                                name += c;
                                if (countingBracketLevel) {
                                    bracketLevel++;
                                } else if (countingCallLevel) {
                                    callLevel++;
                                } else {
                                    tokens.add(new Token(Type.VALUE, name));
                                }

                            } else if (!isAString) {
                                if (name.equals("")) {
                                    name = currentId;
                                    isAString = true;
                                } else {
                                    throw new SyntaxError("Misplaced string bracket (\"" + currentId + "\").", line, i, filePath);
                                }
                            }

                        case VALSEP:
                            if (isAString) break;
                            if (countingBracketLevel) {
                                bracketLevel++;
                            } else if (countingCallLevel) {
                                bracketLevel++;
                            } else {
                                if (isACall) {
                                    isACall = false;
                                    tokens.add(new Token(Type.CALL, name, callContains));
                                    callContains = 0;
                                } else if (isAValue) {
                                    tokens.add(new Token(Type.VALUE, name));
                                } else {
                                    tokens.add(new Token(Type.VARIABLE, name));
                                }

                                name = "";
                                currentId = "";
                            }
                            break;

                        case LINESEP:
                            if (isAString) break;
                            nameOver = true;
                            if (countingBracketLevel || countingCallLevel) {
                                bracketLevel++;
                            } else {
                                tokens.add(new Token(Type.LINE_SEPARATOR, binding(Symbol.LINESEP)));
                                previousTokenCount = tokens.size();
                            } break;

                        case DEFOPEN:
                            if (isAString) break;
                            if (!countingBracketLevel && !countingCallLevel) {
                                countingBracketLevel = true;
                                bracketLevel = 0;
                            }
                            else {
                                bracketLevel++;
                            } break;

                        case DEFCLOSE:
                            if (isAString) break;
                            if (bracketLevel == 0) {
                                countingBracketLevel = false;
                                i = bracketOpenIndex;
                                tokens.add(new Token(Type.DEFINITION, name, callContains, bracketContains));
                            } else {
                                bracketLevel--;
                            } break;

                        case CALLOPEN:
                            if (isAString) break;
                            nameOver = true;
                            if (countingCallLevel || countingBracketLevel) {
                                bracketLevel++;
                            } else {
                                callName = name;
                                countingCallLevel = true;
                                bracketLevel = 0;
                                bracketOpenIndex = i;
                            }

                        case CALLCLOSE:
                            if (isAString) break;
                            nameOver = true;
                            if (countingCallLevel) {
                                if (bracketLevel > 0) {
                                    bracketLevel--;
                                } else {
                                    isACall = true;
                                    countingCallLevel = false;
                                    callContains = bracketContains;
                                    i = bracketOpenIndex;
                                    name = "";
                                    currentId = "";
                                    previousId = "";
                                    previousTokenCount = tokens.size();
                                    continue;
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
                }
            }

            if (name.length() > 0) {
                if (operatorStartsWith(name)) {
                    if (operators.contains(name)) {
                        if (!countingBracketLevel && !countingCallLevel) {
                            tokens.add(new Token(Type.OPERATOR, name));
                            currentId = "" + c;
                        } else {
                            bracketContains++;
                        }
                    }
                }
            }

            if (isAString && !disableNextChar && c == DISABLE_NEXT_CHAR) {
                disableNextChar = true;
            } else {
                disableNextChar = false;
            }

            System.out.println(name + countingCallLevel);
            if (nameOver) {
                if (previousTokenCount == tokens.size() && name.length() > 0) {
                    if (!countingCallLevel && !countingBracketLevel) {
                        tokens.add(new Token(Type.VARIABLE, name));
                    } else {
                        bracketContains++;
                    }
                }

                name = "";
                nameOver = false;
            } else {
                name += c;
            }
            previousTokenCount = tokens.size();
        }
        return tokens;
    }

    private boolean isABinding(String s) {
        return bindings.containsValue(s);
    }

    private boolean bindingStartsWith(String str) {
        for (String i : bindings.keySet()) {
            if (bindings.get(i).startsWith(str)) {
                return true;
            }
        }
        return false;
    }

    private Symbol recognizeBinding(String s) {
        for (Object i : special_character_config_names.keySet()) {
            if (s.equals(bindings.get(special_character_config_names.get(i)))) {
                return (Symbol) i;
            }
        }
        return Symbol.NONE;
    }

    private String binding(Symbol c) {
        return bindings.get(special_character_config_names.get(c));
    }

    private boolean operatorStartsWith(String str) {
        for (String s : operators) {
            if (s.startsWith(str)) {
                return true;
            }
        }
        return false;
    }

    private void init() throws LexerException {
        lineSeparator = ";";
        special_character_config_names = Map.of(
                Symbol.SPACE, "space",
                Symbol.LINESEP, "lineSeparator",
                Symbol.VALSEP, "valueSeparator",
                Symbol.DEFOPEN, "definitionOpen",
                Symbol.DEFCLOSE, "definitionClose",
                Symbol.CALLOPEN, "callOpen",
                Symbol.CALLCLOSE, "callClose",
                Symbol.STRBRACKET, "stringBracket"
        );

        try {
            FileLoader f = new FileLoader();
            config = new JSONObject(new String(f.getFile(config_path)));

            bindings = new HashMap<>();
            Map<String, Object> m = config.getJSONObject("bindings").toMap();
            for (Object i : m.keySet()) {
                bindings.put((String) i, (String) m.get(i));
            }

            operators = new ArrayList<>();
            for (Object i : config.getJSONArray("operators")) {
                operators.add((String) i);
            }
        } catch (Exception e) {
            System.out.println(e);
            throw new LexerException("Failed loading lexer config file (\"" + config_path + "\").");
        }
    }

    public Lexer(String configFile) throws LexerException {
        config_path = configFile;
        init();
    }

    public static void main(String[] args) {
        Lexer l;
        try {
            l = new Lexer("C:\\Users\\Maks\\Documents\\GitHub\\Candor\\web_server\\configuration\\girder\\lexer.json");
            String s;
            //s = "print(\"This\", is + 15) { a (test); }";
            s = "print(hhh + j); print();";
            for (Token i : l.lexInstruction(s, 0, "test.gir")) {
                System.out.println("id: " + i.type + ", value: \"" + i.value + "\", range: " + i.range);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
