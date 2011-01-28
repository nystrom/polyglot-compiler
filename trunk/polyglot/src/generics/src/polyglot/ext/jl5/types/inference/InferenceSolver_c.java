package polyglot.ext.jl5.types.inference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import polyglot.ext.jl5.types.JL5ArrayType;
import polyglot.ext.jl5.types.JL5ProcedureInstance;
import polyglot.ext.jl5.types.JL5TypeSystem;
import polyglot.ext.jl5.types.TypeVariable;
import polyglot.types.ClassType;
import polyglot.types.Type;

public class InferenceSolver_c implements InferenceSolver {

    private JL5TypeSystem ts;

    private JL5ProcedureInstance pi = null;

    private List<Type> actuals;

    private List<Type> formals;

    private List<TypeVariable> typeVariables;

    private Type expRetType = null;

    public InferenceSolver_c(JL5ProcedureInstance pi, List<Type> actuals, JL5TypeSystem ts) {
        this.pi = pi;
        this.typeVariables = pi.typeVariables();
        this.actuals = actuals;
        this.formals = pi.formalTypes();
        this.ts = ts;
    }

    public InferenceSolver_c(List<TypeVariable> typeVars, List<Type> formals, List<Type> actuals,
            JL5TypeSystem ts) {
        this.ts = ts;
        this.actuals = actuals;
        this.formals = formals;
        this.typeVariables = typeVars;
    }

    public boolean isTargetTypeVariable(Type t) {
        if (t instanceof TypeVariable) {
            TypeVariable tv = (TypeVariable) t;
            return typeVariables().contains(tv);
        }
        return false;
    }

    public List<TypeVariable> typeVariables() {
        return typeVariables;
    }

    public List<Type> solve() {
        List<Constraint> constraints = getInitialConstraints();
        List<EqualConstraint> equals = new ArrayList<EqualConstraint>();
        List<SubTypeConstraint> subs = new ArrayList<SubTypeConstraint>();
        List<SuperTypeConstraint> supers = new ArrayList<SuperTypeConstraint>();

        while (!constraints.isEmpty()) {
            Constraint head = constraints.remove(0);
            if (head.canSimplify()) {
                constraints.addAll(0, head.simplify());
            }
            else {
                if (head instanceof EqualConstraint) {
                    EqualConstraint eq = (EqualConstraint) head;
                    equals.add(eq);
                }
                else if (head instanceof SubTypeConstraint) {
                    SubTypeConstraint sub = (SubTypeConstraint) head;
                    subs.add(sub);
                }
                else if (head instanceof SuperTypeConstraint) {
                    SuperTypeConstraint sup = (SuperTypeConstraint) head;
                    supers.add(sup);
                }
            }
        }
        Comparator<Constraint> comp = new Comparator<Constraint>() {
            public int compare(Constraint o1, Constraint o2) {
                return typeVariables().indexOf(o1) - typeVariables().indexOf(o2);
            }
        };
        Collections.sort(equals, comp);
        Collections.sort(subs, comp);

        Type[] solution = new Type[typeVariables().size()];
        for (EqualConstraint eq : equals) {
            int i = typeVariables().indexOf(eq.formal);
            if ((solution[i] != null) && (!ts.equals(eq.actual, solution[i]))) {
                solution[i] = ts.Object();
            }
            else {
                solution[i] = eq.actual;
            }
        }
        for (int i = 0; i < solution.length; i++) {
            if (solution[i] == null) {
                TypeVariable toSolve = typeVariables().get(i);
                Set<ClassType> uset = new HashSet<ClassType>();
                for (Constraint c : subs) {
                    if (c.formal.equals(toSolve))
                        uset.add((ClassType) c.actual);
                }
                List<ClassType> u = new ArrayList<ClassType>(uset);
                if (u.size() == 1) {
                    solution[i] = u.get(0);
                }
                else if (u.size() > 1) {
                    solution[i] = ts.lubType(u);
                }
            }
        }
        for (int i = 0; i < solution.length; i++)
            if (solution[i] == null)
                solution[i] = ts.Object();
        List<Type> r = new ArrayList<Type>();
        Collections.addAll(r, solution);
        return r;
    }

    private List<Constraint> getInitialConstraints() {
        List<Constraint> constraints = new ArrayList<Constraint>();
        int numFormals = formals.size();
        for (int i = 0; i < numFormals - 1; i++) {
            constraints.add(new SubConversionConstraint(actuals.get(i), formals.get(i), this));
        }
        if (numFormals > 0) {
            if (pi != null && pi.isVariableArrity()) {
                JL5ArrayType lastFormal = (JL5ArrayType) pi.formalTypes().get(numFormals - 1);
                for (int i = numFormals - 1; i < actuals.size() - 1; i++) {
                    constraints.add(new SubConversionConstraint(actuals.get(i), lastFormal.base(), this));
                }
            }
            constraints.add(new SubConversionConstraint(actuals.get(numFormals - 1), formals.get(numFormals - 1), this));
        }
        return constraints;
    }

    public List<Type> solve(Type expectedReturnType) {
        this.expRetType = expectedReturnType;
        return solve();
    }

    public JL5TypeSystem typeSystem() {
        return ts;
    }

    public JL5ProcedureInstance procedureInstance() {
        return pi;
    }

}
