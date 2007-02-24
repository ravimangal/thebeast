package thebeast.nod;

import thebeast.nod.statement.StatementFactory;
import thebeast.nod.statement.Interpreter;
import thebeast.nod.variable.VariableFactory;
import thebeast.nod.type.TypeFactory;
import thebeast.nod.expression.ExpressionFactory;
import thebeast.nod.identifier.IdentifierFactory;
import thebeast.nod.util.ExpressionBuilder;

import java.util.List;

/**
 * @author Sebastian Riedel
 */
public interface NoDServer {
  VariableFactory variableFactory();

  TypeFactory typeFactory();

  StatementFactory statementFactory();

  ExpressionFactory expressionFactory();

  ExpressionBuilder expressionBuilder();

  IdentifierFactory identifierFactory();

  Interpreter interpreter();

}