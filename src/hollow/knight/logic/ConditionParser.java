package hollow.knight.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;

public final class ConditionParser {
  private interface Atom {
    public enum Type {
      CONDITION, TERM, DISJUNCTION_OPERATOR, CONJUNCTION_OPERATOR, LEFT_PAREN, RIGHT_PAREN, GREATER_THAN, LESS_THAN, EQUAL_TO, NUMERIC_LITERAL,
    };

    Type type();
  }

  private static final class SingletonType implements Atom {
    private final Type type;

    private SingletonType(Type type) {
      this.type = type;
    }

    @Override
    public Type type() {
      return type;
    }
  }

  private interface ConditionAtom extends Atom {
    Condition condition();
  }

  private static final class LogicConditionAtom implements ConditionAtom {
    private final Condition condition;

    public LogicConditionAtom(Condition condition) {
      this.condition = condition;
    }

    @Override
    public Type type() {
      return Type.CONDITION;
    }

    @Override
    public Condition condition() {
      return condition;
    }
  }

  private static final class TermAtom implements ConditionAtom {
    private final TermGreaterThanCondition condition;

    public TermAtom(Term term) {
      this.condition = TermGreaterThanCondition.of(term);
    }

    @Override
    public Type type() {
      return Type.TERM;
    }

    public Term term() {
      return condition.term();
    }

    @Override
    public Condition condition() {
      return condition;
    }
  }

  private static final class NumericAtom implements Atom {
    private final int value;

    public NumericAtom(int value) {
      this.value = value;
    }

    @Override
    public Type type() {
      return Type.NUMERIC_LITERAL;
    }

    public int value() {
      return value;
    }
  }

  private static final ImmutableList<Atom.Type> OPERATION_ORDER =
      ImmutableList.of(Atom.Type.GREATER_THAN, Atom.Type.LESS_THAN, Atom.Type.EQUAL_TO,
          Atom.Type.CONJUNCTION_OPERATOR, Atom.Type.DISJUNCTION_OPERATOR);

  private static final Atom OR = new SingletonType(Atom.Type.DISJUNCTION_OPERATOR);
  private static final Atom AND = new SingletonType(Atom.Type.CONJUNCTION_OPERATOR);
  private static final Atom LPAREN = new SingletonType(Atom.Type.LEFT_PAREN);
  private static final Atom RPAREN = new SingletonType(Atom.Type.RIGHT_PAREN);
  private static final Atom GT = new SingletonType(Atom.Type.GREATER_THAN);
  private static final Atom LT = new SingletonType(Atom.Type.LESS_THAN);
  private static final Atom EQ = new SingletonType(Atom.Type.EQUAL_TO);

  public static Condition parse(String text) throws ParseException {
    ConditionParser parser = new ConditionParser();
    try {
      return parse(parser.parseAtoms(text));
    } catch (ParseException ex) {
      throw new ParseException("Failed to parse '" + text + "': " + ex.getMessage());
    }
  }

  private static Condition parse(List<Atom> atoms) throws ParseException {
    // Handle parenthesis
    atoms = parseParenthesis(atoms);

    // Handle binary operations
    for (Atom.Type op : OPERATION_ORDER) {
      atoms = parseBinaryOp(op, atoms);
    }

    if (atoms.size() != 1 || !(atoms.get(0) instanceof ConditionAtom)) {
      throw new ParseException("Incomplete expression");
    }

    return ((ConditionAtom) atoms.get(0)).condition();
  }

  private static List<Atom> parseParenthesis(List<Atom> atoms) throws ParseException {
    int stack = 0;
    List<Atom> newList = new ArrayList<>();
    List<Atom> subList = new ArrayList<>();
    for (Atom atom : atoms) {
      switch (atom.type()) {
        case LEFT_PAREN:
          stack++;
          if (stack > 1) {
            subList.add(atom);
          }
          break;
        case RIGHT_PAREN:
          stack--;
          if (stack < 0) {
            throw new ParseException("Unmatched Right Paren");
          } else if (stack > 0) {
            subList.add(atom);
          } else {
            newList.add(new LogicConditionAtom(parse(subList)));
            subList.clear();
          }
          break;
        default:
          if (stack > 0) {
            subList.add(atom);
          } else {
            newList.add(atom);
          }
          break;
      }
    }

    if (stack > 0 || !subList.isEmpty()) {
      throw new ParseException("Unmatched Left Paren");
    }

    return newList;
  }

  private static List<Atom> parseBinaryOp(Atom.Type op, List<Atom> atoms) throws ParseException {
    List<Atom> newAtoms = new ArrayList<>();
    for (int i = 0; i < atoms.size(); i++) {
      Atom atom = atoms.get(i);
      if (atom.type() != op) {
        newAtoms.add(atom);
        continue;
      }

      // Handle left-associative op.
      if (i == 0 || i == atoms.size() - 1) {
        throw new ParseException("Operator with missing operand");
      }

      Condition opCond = parseBinaryOp(op, newAtoms.get(newAtoms.size() - 1), atoms.get(i + 1));
      newAtoms.set(newAtoms.size() - 1, new LogicConditionAtom(opCond));
      i++;
    }

    return newAtoms;
  }

  private static Condition parseBinaryOp(Atom.Type op, Atom left, Atom right)
      throws ParseException {
    switch (op) {
      case CONJUNCTION_OPERATOR:
      case DISJUNCTION_OPERATOR: {
        if (!(left instanceof ConditionAtom) || !(right instanceof ConditionAtom)) {
          throw new ParseException("Unsupported logic operator");
        }

        Condition lCond = ((ConditionAtom) left).condition();
        Condition rCond = ((ConditionAtom) right).condition();

        if (op == Atom.Type.CONJUNCTION_OPERATOR) {
          return Conjunction.of(lCond, rCond);
        } else {
          return Disjunction.of(lCond, rCond);
        }
      }
      case GREATER_THAN:
      case LESS_THAN:
      case EQUAL_TO: {
        if (op == Atom.Type.GREATER_THAN && left.type() == Atom.Type.TERM
            && right.type() == Atom.Type.TERM) {
          TermAtom lTerm = (TermAtom) left;
          TermAtom rTerm = (TermAtom) right;

          Optional<Condition> cond = NotchCostCondition.tryParse(lTerm.term(), rTerm.term());
          if (cond.isPresent()) {
            return cond.get();
          }
        }
        if (left.type() != Atom.Type.TERM || right.type() != Atom.Type.NUMERIC_LITERAL) {
          throw new ParseException("Unsupported operator (" + left + " " + op + " " + right + ")");
        }

        Term term = ((TermAtom) left).term();
        int value = ((NumericAtom) right).value();
        if (op == Atom.Type.GREATER_THAN) {
          return TermGreaterThanCondition.of(term, value);
        } else if (op == Atom.Type.LESS_THAN) {
          return TermLessThanCondition.of(term, value);
        } else {
          return TermEqualToCondition.of(term, value);
        }
      }
      default:
        throw new ParseException("Internal error: not a binary op");
    }
  }

  private StringBuilder currentAtom = new StringBuilder();
  private final List<Atom> atoms = new ArrayList<>();
  private boolean bracket = false;

  private List<Atom> parseAtoms(String text) throws ParseException {
    for (char ch : text.toCharArray()) {
      if (ch == ' ' && !bracket) {
        closeAtom();
      } else if (ch == '|') {
        closeAtom(OR);
      } else if (ch == '+') {
        closeAtom(AND);
      } else if (ch == '(') {
        closeAtom(LPAREN);
      } else if (ch == ')') {
        closeAtom(RPAREN);
      } else if (ch == '>') {
        closeAtom(GT);
      } else if (ch == '<') {
        closeAtom(LT);
      } else if (ch == '=') {
        closeAtom(EQ);
      } else {
        currentAtom.append(ch);
        if (ch == '[') {
          if (bracket) {
            throw new ParseException("Nested brackets?");
          }
          bracket = true;
        } else if (ch == ']') {
          if (!bracket) {
            throw new ParseException("Unmatched right bracket");
          }
          bracket = false;
        }
      }
    }
    closeAtom();

    return atoms;
  }

  private void closeAtom(Atom next) throws ParseException {
    if (bracket) {
      throw new ParseException("Unclosed bracket");
    }

    String content = currentAtom.toString();
    currentAtom = new StringBuilder();

    if (!content.isEmpty()) {
      Integer number = Ints.tryParse(content);
      if (number != null) {
        atoms.add(new NumericAtom(number));
      } else {
        atoms.add(new TermAtom(Term.create(content)));
      }
    }
    if (next != null) {
      atoms.add(next);
    }
  }

  private void closeAtom() throws ParseException {
    closeAtom(null);
  }

  private ConditionParser() {}
}
