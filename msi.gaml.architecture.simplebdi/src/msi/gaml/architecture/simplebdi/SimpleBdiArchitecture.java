/*********************************************************************************************
 * 
 * 
 * 'SimpleBdiArchitecture.java', in plugin 'msi.gaml.architecture.simplebdi', is part of the source code of the
 * GAMA modeling and simulation platform.
 * (c) 2007-2014 UMI 209 UMMISCO IRD/UPMC & Partners
 * 
 * Visit https://code.google.com/p/gama-platform/ for license information and developers contact.
 * 
 * 
 **********************************************************************************************/
package msi.gaml.architecture.simplebdi;

import java.util.*;
import msi.gama.metamodel.agent.IAgent;
import msi.gama.precompiler.GamlAnnotations.action;
import msi.gama.precompiler.GamlAnnotations.arg;
import msi.gama.precompiler.GamlAnnotations.doc;
import msi.gama.precompiler.GamlAnnotations.example;
import msi.gama.precompiler.GamlAnnotations.skill;
import msi.gama.precompiler.GamlAnnotations.var;
import msi.gama.precompiler.GamlAnnotations.vars;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gama.util.*;
import msi.gaml.architecture.reflex.ReflexArchitecture;
import msi.gaml.compilation.ISymbol;
import msi.gaml.operators.Cast;
import msi.gaml.species.ISpecies;
import msi.gaml.statements.IStatement;
import msi.gaml.types.*;

@vars({ @var(name = SimpleBdiArchitecture.PERSISTENCE_COEFFICIENT_PLANS, type = IType.FLOAT, init = "1.0", doc= @doc ("plan persistence")),
	@var(name = SimpleBdiArchitecture.PERSISTENCE_COEFFICIENT_GOALS, type = IType.FLOAT, init = "1.0", doc= @doc ("goal persistence")),
	@var(name = SimpleBdiArchitecture.PROBABILISTIC_CHOICE, type = IType.BOOL, init = "true"),
	@var(name = SimpleBdiArchitecture.BELIEF_BASE, type = IType.LIST, init = "[]"),
	@var(name = SimpleBdiArchitecture.LAST_THOUGHTS, type = IType.LIST, init = "[]"),
	@var(name = SimpleBdiArchitecture.INTENSION_BASE, type = IType.LIST, init = "[]"),
	@var(name = SimpleBdiArchitecture.DESIRE_BASE, type = IType.LIST, init = "[]") })
@skill(name = SimpleBdiArchitecture.SIMPLE_BDI)

public class SimpleBdiArchitecture extends ReflexArchitecture {

	public static final String SIMPLE_BDI = "simple_bdi";
	public static final String PLAN = "plan";
	public static final String PRIORITY = "priority";
	public static final String FINISHEDWHEN = "finished_when";
	public static final String PERSISTENCE_COEFFICIENT_PLANS = "plan_persistence";
	public static final String PERSISTENCE_COEFFICIENT_GOALS = "goal_persistence";	
	
	//TODO: Not implemented yet
	public static final String PROBABILISTIC_CHOICE = "probabilistic_choice";
	public static final String INSTANTANEAOUS = "instantaneaous";

	//INFORMATION THAT CAN BE DISPLAYED
	public static final String LAST_THOUGHTS = "thinking";
	public static final Integer LAST_THOUGHTS_SIZE = 5;
	
	
	public static final String PREDICATE = "predicate";
	public static final String PREDICATE_NAME = "name";
	public static final String PREDICATE_VALUE = "value";
	public static final String PREDICATE_PRIORITY = "priority";
	public static final String PREDICATE_PARAMETERS = "parameters";
	public static final String PREDICATE_ONHOLD = "on_hold_until";
	public static final String PREDICATE_TODO = "todo";
	public static final String PREDICATE_SUBGOALS = "subgoals";
	public static final String PREDICATE_DATE = "date";
	public static final String BELIEF_BASE = "belief_base";
	public static final String REMOVE_DESIRE_AND_INTENSION = "desire_also";
	public static final String DESIRE_BASE = "desire_base";
	public static final String INTENSION_BASE = "intension_base";
	public static final String EVERY_VALUE = "every_possible_value_";

	private IScope _consideringScope;
	private final List<SimpleBdiPlan> _plans = new ArrayList<SimpleBdiPlan>();
	private final List<SimpleBdiPlan> _perceives = new ArrayList<SimpleBdiPlan>();
	private int _plansNumber = 0;
	private int _perceiveNumber = 0;
	private SimpleBdiPlan _persistentTask = null;

	@Override
	public void setChildren(final List<? extends ISymbol> children) {
		super.setChildren(children);
		for ( ISymbol c : children ) {
			addBehavior((IStatement) c);
		}
	}

	@Override
	public void addBehavior(final IStatement c) {
		super.addBehavior(c);
		if ( c instanceof SimpleBdiPlan ) {
			String statementKeyword = c.getFacet("keyword").value(_consideringScope).toString();
			_plans.add((SimpleBdiPlan) c);
			_plansNumber++;
		}
	}

	@Override
	public Object executeOn(final IScope scope) throws GamaRuntimeException {
		super.executeOn(scope);
		return executePlans(scope);
	}

	protected final Object executePlans(final IScope scope) {
		Object result = null;
		if ( _perceiveNumber > 0 ) {
			for ( int i = 0; i < _perceiveNumber; i++ ) {
				result = _perceives.get(i).executeOn(scope);
			}
		}
		if ( _plansNumber > 0 ) {
			final IAgent agent = getCurrentAgent(scope);
			GamaList<Predicate> desireBase =
				(GamaList<Predicate>) (scope.hasArg(DESIRE_BASE) ? scope.getListArg(DESIRE_BASE)
					: (GamaList<Predicate>) agent.getAttribute(DESIRE_BASE));
			GamaList<Predicate> intensionBase =
				(GamaList<Predicate>) (scope.hasArg(INTENSION_BASE) ? scope.getListArg(INTENSION_BASE)
					: (GamaList<Predicate>) agent.getAttribute(INTENSION_BASE));

			Double persistenceCoefficientPlans =
				scope.hasArg(PERSISTENCE_COEFFICIENT_PLANS) ? scope.getFloatArg(PERSISTENCE_COEFFICIENT_PLANS) : (Double) agent
					.getAttribute(PERSISTENCE_COEFFICIENT_PLANS);
			Double persistenceCoefficientgoal =
				scope.hasArg(PERSISTENCE_COEFFICIENT_GOALS) ? scope.getFloatArg(PERSISTENCE_COEFFICIENT_GOALS)
					: (Double) agent.getAttribute(PERSISTENCE_COEFFICIENT_GOALS);

			// RANDOMLY REMOVE (last)INTENSION
			Boolean flipResultgoal = msi.gaml.operators.Random.opFlip(scope, persistenceCoefficientgoal);
			while (!flipResultgoal) {
				flipResultgoal = msi.gaml.operators.Random.opFlip(scope,
						persistenceCoefficientgoal);
				if (intensionBase.size() > 0) {
					int toremove=intensionBase.size()-1;
					Predicate previousint = intensionBase.get(toremove);
					intensionBase.remove(toremove);
					String think="check what happens if I remove: "
							+ previousint;					
					addThoughts(scope, think);
					_persistentTask = null;
				}
			}

			// If current intension has no plan or is on hold, choose a new
			// Desire
			if ( testOnHold(scope, currentGoal(scope)) || selectExecutablePlanWithHighestPriority(scope) == null ) {
				selectDesireWithHighestPriority(scope);
				_persistentTask = null;

			}

			Boolean flipResult = msi.gaml.operators.Random.opFlip(scope, persistenceCoefficientPlans);

			if ( !flipResult ) {
				if ( _persistentTask != null ) {
					addThoughts(scope, "check what happens if I stop: " + _persistentTask.getName());
				}
				_persistentTask = selectExecutablePlanWithHighestPriority(scope);

				if ( _persistentTask != null ) {
					addThoughts(scope, "lets do instead " + _persistentTask.getName());
				}

			}

			// choose a plan for the current goal

			if ( _persistentTask == null && currentGoal(scope) == null ) {
				selectDesireWithHighestPriority(scope);
				_persistentTask = selectExecutablePlanWithHighestPriority(scope);
				if ( currentGoal(scope) == null ) {
					addThoughts(scope, "I want nothing...");
					return null;

				}
				addThoughts(scope, "ok, new goal: " + currentGoal(scope) + " with plan " + _persistentTask.getName());
			}
			if ( _persistentTask == null && currentGoal(scope) != null ) {
				_persistentTask = selectExecutablePlanWithHighestPriority(scope);
				if ( _persistentTask != null ) {
					addThoughts(scope, "use plan : " + _persistentTask.getName());
				}
			}
			if ( _persistentTask != null ) {
				if ( !agent.dead() ) {
					result = _persistentTask.executeOn(scope);
					boolean isExecuted =
						_persistentTask.getExecutedExpression() == null ||
							msi.gaml.operators.Cast.asBool(scope, _persistentTask.getExecutedExpression().value(scope));
					if ( isExecuted ) {
						_persistentTask = null;

					}
					if ( getBase(scope, BELIEF_BASE).contains(currentGoal(scope)) ) {
						addThoughts(scope, "goal " + currentGoal(scope) + " reached! abort current plan... ");
						removeFromBase(scope, currentGoal(scope), DESIRE_BASE);
						removeFromBase(scope, currentGoal(scope), INTENSION_BASE);

						_persistentTask = null;

					}

				}
			}
		}

		return result;
	}

	protected final Boolean selectDesireWithHighestPriority(final IScope scope) {
		GamaList<Predicate> desireBase =
			(GamaList<Predicate>) scope.getExperiment().getRandomGenerator().shuffle(getBase(scope, DESIRE_BASE));
		GamaList<Predicate> intensionBase = getBase(scope, INTENSION_BASE);
		double maxpriority = Double.NEGATIVE_INFINITY;
		if ( desireBase.size() > 0 && intensionBase != null ) {
			Predicate newGoal = desireBase.anyValue(scope);
			for ( Predicate desire : desireBase ) {
				if ( desire.priority > maxpriority ) {
					if ( !intensionBase.contains(desire) ) {
						maxpriority = desire.priority;
						newGoal = desire;

					}
				}
			}
			if ( !intensionBase.contains(newGoal) ) {
				intensionBase.add(newGoal);
				return true;
			}
		}
		return false;
	}

	protected final SimpleBdiPlan selectExecutablePlanWithHighestPriority(final IScope scope) {
		SimpleBdiPlan resultStatement = null;
		double highestPriority = Double.MIN_VALUE;
		for ( Object statement : scope.getExperiment().getRandomGenerator().shuffle(_plans) ) {
			boolean isContextConditionSatisfied =
				((SimpleBdiPlan) statement).getContextExpression() == null ||
					msi.gaml.operators.Cast.asBool(scope, ((SimpleBdiPlan) statement).getContextExpression()
						.value(scope));
			if ( isContextConditionSatisfied ) {
				double currentPriority =
					msi.gaml.operators.Cast.asFloat(scope, ((SimpleBdiPlan) statement).getPriorityExpression()
						.value(scope));
				if ( highestPriority < currentPriority ) {
					highestPriority = currentPriority;
					resultStatement = (SimpleBdiPlan) statement;
				}
			}
		}
		return resultStatement;
	}

	public GamaList<String> getThoughts(final IScope scope) {
		final IAgent agent = getCurrentAgent(scope);
		GamaList<String> thoughts = (GamaList<String>) agent.getAttribute(LAST_THOUGHTS);
		return thoughts;
	}

	public IList<String> addThoughts(final IScope scope, final String think) {
		final IAgent agent = getCurrentAgent(scope);
		GamaList<String> thoughts = (GamaList<String>) agent.getAttribute(LAST_THOUGHTS);
		IList newthoughts = GamaListFactory.create(Types.STRING);
		newthoughts.add(think);
		if ( thoughts != null && thoughts.size() > 0 ) {
			newthoughts.addAll(thoughts.subList(0, Math.min(LAST_THOUGHTS_SIZE - 1, thoughts.size())));
		}
		agent.setAttribute(LAST_THOUGHTS, newthoughts);
		return newthoughts;
	}

	public boolean testOnHold(final IScope scope, final Predicate goal) {
		if ( goal == null ) { return false; }
		if ( goal.onHoldUntil == null ) { return false; }
		Object cond = goal.onHoldUntil;
		if ( cond instanceof GamaList ) {
			GamaList desbase = getBase(scope, DESIRE_BASE);
			if ( desbase.isEmpty() ) { return false; }
			for ( Object subgoal : (GamaList) cond ) {
				if ( desbase.contains(subgoal) ) { return true; }
			}
			addThoughts(scope, "no more subgoals for" + goal);
			return false;
		}
		if ( cond instanceof String ) {
			Object res = msi.gaml.operators.System.opEvalGaml(scope, (String) cond);
			if ( Cast.asBool(scope, res) == false ) { return true; }

		}
		return false;

	}

	public GamaList<Predicate> getBase(final IScope scope, final String basename) {
		final IAgent agent = getCurrentAgent(scope);
		return (GamaList<Predicate>) (scope.hasArg(basename) ? scope.getListArg(basename) : (GamaList<Predicate>) agent
			.getAttribute(basename));
	}

	public boolean removeFromBase(final IScope scope, final Predicate predicateItem, final String factBaseName) {
		GamaList<Predicate> factBase = getBase(scope, factBaseName);
		return factBase.remove(predicateItem);
	}

	public boolean addToBase(final IScope scope, final Predicate predicateItem, final String factBaseName) {
		return addToBase(scope, predicateItem, getBase(scope, factBaseName));
	}

	public boolean addToBase(final IScope scope, final Predicate predicateItem, final GamaList<Predicate> factBase) {
		factBase.remove(predicateItem);

		predicateItem.setDate(scope.getClock().getTime());
		return factBase.add(predicateItem);
	}

	@action(name = "add_belief", args = { @arg(name = PREDICATE,
		type = IType.MAP,
		optional = true,
		doc = @doc("predicate to check")) }, doc = @doc(value = "check if the predicates is in the desire base.",
		returns = "true if it is in the base.",
		examples = { @example("") }))
	// @args(names = { PREDICATE_NAME, PREDICATE_PARAMETERS })
		public
		Boolean primAddBelief(final IScope scope) throws GamaRuntimeException {
		Predicate predicateDirect =
			(Predicate) (scope.hasArg(PREDICATE) ? scope.getArg(PREDICATE, PredicateType.id) : null);
		if ( predicateDirect != null ) { return addToBase(scope, predicateDirect, BELIEF_BASE); }

		return false;

	}

	@action(name = "has_belief", args = { @arg(name = PREDICATE,
		type = PredicateType.id,
		optional = true,
		doc = @doc("predicate to check")) }, doc = @doc(value = "check if the predicates is in the belief base.",
		returns = "true if it is in the base.",
		examples = { @example("") }))
	// @args(names = { PREDICATE_NAME, PREDICATE_PARAMETERS })
		public
		Boolean primTestBelief(final IScope scope) throws GamaRuntimeException {
		Predicate predicateDirect =
			(Predicate) (scope.hasArg(PREDICATE) ? scope.getArg(PREDICATE, PredicateType.id) : null);
		if ( predicateDirect != null ) { return getBase(scope, BELIEF_BASE).contains(predicateDirect);

		}
		return false;
	}

	@action(name = "get_belief", args = { @arg(name = PREDICATE,
		type = PredicateType.id,
		optional = true,
		doc = @doc("predicate to check")) }, doc = @doc(value = "get the predicates is in the belief base.",
		returns = "the predicate if it is in the base.",
		examples = { @example("") }))
	// @args(names = { PREDICATE_NAME, PREDICATE_PARAMETERS })
		public
		Predicate getBelief(final IScope scope) throws GamaRuntimeException {
		Predicate predicateDirect =
			(Predicate) (scope.hasArg(PREDICATE) ? scope.getArg(PREDICATE, PredicateType.id) : null);
		if ( predicateDirect != null ) {
			for ( Predicate pred : getBase(scope, BELIEF_BASE) ) {
				if ( predicateDirect.equals(pred) ) { return pred; }
			}

		}
		return null;

	}

	@action(name = "is_current_goal",
		args = { @arg(name = PREDICATE, type = PredicateType.id, optional = false, doc = @doc("predicate to check")) },
		doc = @doc(value = "check if the predicates is the current goal (last entry of intension base).",
			returns = "true if it is in the base.",
			examples = { @example("") }))
	public Boolean iscurrentGoal(final IScope scope) throws GamaRuntimeException {
		Predicate predicateDirect =
			(Predicate) (scope.hasArg(PREDICATE) ? scope.getArg(PREDICATE, PredicateType.id) : null);
		Predicate currentGoal = currentGoal(scope);

		if ( predicateDirect != null && currentGoal != null ) { return predicateDirect.equals(currentGoal); }

		return false;
	}

	@action(name = "get_current_goal", doc = @doc(value = "returns the current goal (last entry of intension base).",
		returns = "true if it is in the base.",
		examples = { @example("") }))
	public Predicate currentGoal(final IScope scope) throws GamaRuntimeException {
		GamaList<Predicate> intensionBase = getBase(scope, INTENSION_BASE);
		if ( intensionBase == null ) { return null; }
		if ( !intensionBase.isEmpty() ) { return intensionBase.lastValue(scope); }
		return null;
	}

	@action(name = "has_desire", args = { @arg(name = PREDICATE,
		type = PredicateType.id,
		optional = true,
		doc = @doc("predicate to check")) }, doc = @doc(value = "check if the predicates is in the desire base.",
		returns = "true if it is in the base.",
		examples = { @example("") }))
	// @args(names = { PREDICATE_NAME, PREDICATE_PARAMETERS })
		public
		Boolean primTestDesire(final IScope scope) throws GamaRuntimeException {
		Predicate predicateDirect =
			(Predicate) (scope.hasArg(PREDICATE) ? scope.getArg(PREDICATE, PredicateType.id) : null);
		if ( predicateDirect != null ) { return getBase(scope, DESIRE_BASE).contains(predicateDirect);

		}
		return false;
	}

	@action(name = "currentgoal_on_hold",
		args = { @arg(name = PREDICATE_ONHOLD,
			type = IType.NONE,
			optional = true,
			doc = @doc("the specified intension is put on hold (fited plan are not considered) until specific condition is reached. Can be an expression (which will be tested), a list (of subgoals), or nil (by default the condition will be the current list of subgoals of the intension)")) },
		doc = @doc(value = "puts the current goal on hold until the specified condition is reached or all subgoals are reached (not in desire base anymore).",
			returns = "true if it is in the base.",
			examples = { @example("") }))
	public
		Boolean primOnHoldIntension(final IScope scope) throws GamaRuntimeException {
		Predicate predicate = currentGoal(scope);
		Object until = scope.hasArg(PREDICATE_ONHOLD) ? scope.getArg(PREDICATE_ONHOLD, IType.NONE) : null;
		if ( until == null ) {
			List<Predicate> subgoal = predicate.subgoals;
			if ( subgoal != null && !subgoal.isEmpty() ) {
				predicate.onHoldUntil = subgoal;

			}
		} else {
			predicate.onHoldUntil = until;
		}
		return true;
	}

	@action(name = "add_subgoal",
		args = {
			@arg(name = PREDICATE, type = PredicateType.id, optional = false, doc = @doc("predicate name")),
			@arg(name = PREDICATE_SUBGOALS,
				type = PredicateType.id,
				optional = false,
				doc = @doc("the subgoal to add to the predicate")) },
		doc = @doc(value = "adds the predicates is in the desire base.",
			returns = "true if it is in the base.",
			examples = { @example("") }))
	public Boolean addSubGoal(final IScope scope) throws GamaRuntimeException {
		Predicate predicate = (Predicate) (scope.hasArg(PREDICATE) ? scope.getArg(PREDICATE, PredicateType.id) : null);
		Predicate subpredicate =
			(Predicate) (scope.hasArg(PREDICATE_SUBGOALS) ? scope.getArg(PREDICATE_SUBGOALS, PredicateType.id) : null);

		if ( predicate == null || subpredicate == null ) { return false; }

		if ( predicate.getSubgoals() == null ) {
			predicate.subgoals = GamaListFactory.create(Types.get(PredicateType.id));
		} else {
			predicate.getSubgoals().remove(subpredicate);
		}
		predicate.getSubgoals().add(subpredicate);

		return true;
	}

	@action(name = "add_desire",
		args = {
			@arg(name = PREDICATE, type = PredicateType.id, optional = false, doc = @doc("predicate to add")),
			@arg(name = PREDICATE_TODO,
				type = PredicateType.id,
				optional = true,
				doc = @doc("add the desire as a subgoal of this parameter")) },
		doc = @doc(value = "adds the predicates is in the desire base.",
			returns = "true if it is in the base.",
			examples = { @example("") }))
	public Boolean primAddDesire(final IScope scope) throws GamaRuntimeException {
		Predicate predicateDirect =
			(Predicate) (scope.hasArg(PREDICATE) ? scope.getArg(PREDICATE, PredicateType.id) : null);
		if ( predicateDirect != null ) {
			Predicate superpredicate =
				(Predicate) (scope.hasArg(PREDICATE_TODO) ? scope.getArg(PREDICATE_TODO, PredicateType.id) : null);
			if ( superpredicate != null ) {
				if ( superpredicate.getSubgoals() == null ) {
					superpredicate.subgoals = GamaListFactory.create(Types.get(PredicateType.id));

				}
				superpredicate.getSubgoals().add(predicateDirect);

			}
			addToBase(scope, predicateDirect, DESIRE_BASE);
			return true;
		}

		return false;
	}

	@action(name = "remove_belief", args = { @arg(name = PREDICATE,
		type = PredicateType.id,
		optional = true,
		doc = @doc("predicate to add")) }, doc = @doc(value = "removes the predicates from the belief base.",
		returns = "true if it is in the base.",
		examples = { @example("") }))
	public Boolean primRemoveBelief(final IScope scope) throws GamaRuntimeException {
		Predicate predicateDirect =
			(Predicate) (scope.hasArg(PREDICATE) ? scope.getArg(PREDICATE, PredicateType.id) : null);
		if ( predicateDirect != null ) { return getBase(scope, BELIEF_BASE).remove(predicateDirect);

		}
		return false;
	}

	@action(name = "remove_desire", args = { @arg(name = PREDICATE,
		type = PredicateType.id,
		optional = true,
		doc = @doc("predicate to add")) }, doc = @doc(value = "removes the predicates from the desire base.",
		returns = "true if it is in the base.",
		examples = { @example("") }))
	public Boolean primRemoveDesire(final IScope scope) throws GamaRuntimeException {
		Predicate predicateDirect =
			(Predicate) (scope.hasArg(PREDICATE) ? scope.getArg(PREDICATE, PredicateType.id) : null);
		if ( predicateDirect != null ) { return getBase(scope, DESIRE_BASE).remove(predicateDirect);

		}
		return false;
	}

	@action(name = "remove_intention",
		args = {
			@arg(name = PREDICATE, type = PredicateType.id, optional = true, doc = @doc("predicate to add")),
			@arg(name = REMOVE_DESIRE_AND_INTENSION,
				type = IType.BOOL,
				optional = false,
				doc = @doc("removes also desire")) },
		doc = @doc(value = "removes the predicates from the desire base.",
			returns = "true if it is in the base.",
			examples = { @example("") }))
	public Boolean primRemoveIntention(final IScope scope) throws GamaRuntimeException {

		Predicate predicateDirect =
			(Predicate) (scope.hasArg(PREDICATE) ? scope.getArg(PREDICATE, PredicateType.id) : null);
		if ( predicateDirect != null ) {
			Boolean dodesire =
				scope.hasArg(REMOVE_DESIRE_AND_INTENSION) ? scope.getBoolArg(REMOVE_DESIRE_AND_INTENSION) : false;
			getBase(scope, INTENSION_BASE).remove(predicateDirect);
			if ( dodesire ) {
				getBase(scope, DESIRE_BASE).remove(predicateDirect);
			}
			return true;
		}

		return false;
	}

	@Override
	public boolean init(final IScope scope) throws GamaRuntimeException {
		super.init(scope);
		_consideringScope = scope;
		return true;
	}

	@Override
	public void verifyBehaviors(final ISpecies context) {}

	@Override
	public void dispose() {}

}
