package hlaaftana.kismet.call

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import hlaaftana.kismet.Kismet
import hlaaftana.kismet.exceptions.KismetEvaluationException
import hlaaftana.kismet.exceptions.UndefinedVariableException
import hlaaftana.kismet.parser.Parser
import hlaaftana.kismet.parser.StringEscaper
import hlaaftana.kismet.vm.Context
import hlaaftana.kismet.vm.IKismetObject

@CompileStatic
abstract class Expression {
	int ln, cl
	abstract IKismetObject evaluate(Context c)
	Collection<Expression> getMembers() { [] }
	Expression getAt(int i) { members[i] }
	Expression join(Collection<Expression> exprs) {
		throw new UnsupportedOperationException("Cannot join exprs $exprs on class ${this.class}")
	}

	Expression percentize(Parser p) {
		new StaticExpression(this, p.context)
	}

	String repr() { "expr(${this.class})" }

	String toString() { repr() }
}

@CompileStatic
class PathExpression extends Expression {
	Expression root
	List<Step> steps

	PathExpression(Expression root, List<Step> steps) {
		this.root = root
		this.steps = steps
	}

	IKismetObject evaluate(Context c) {
		if (null == root || root instanceof NoExpression) {
			Kismet.model(new PathFunction(c, steps))
		} else {
			applySteps(c, root.evaluate(c), steps)
		}
	}

	static class PathFunction extends Function {
		Context context
		List<Step> steps

		PathFunction(Context context, List<Step> steps) {
			this.context = context
			this.steps = steps
		}

		IKismetObject call(IKismetObject... args) {
			applySteps(context, args[0], steps)
		}
	}

	static IKismetObject applySteps(Context c, IKismetObject object, List<Step> steps) {
		for (step in steps) object = step.apply(c, object)
		object
	}

	String repr() { root.repr() + steps.join('') }

	Collection<Expression> getMembers() {
		def result = new ArrayList<Expression>(steps.size() + 1)
		result.add(root)
		for (def s: steps) result.add(s.asExpr())
		result
	}

	PathExpression join(Collection<Expression> m) {
		def result = new ArrayList<Step>(m.size() - 1)
		final s = steps
		assert s.size() == m.size(), "Members must be same size as joined expressions"
		for (int i = 1; i < m.size(); ++i) {
			result.add(s[i - 1].borrow(m[i]))
		}
		new PathExpression(m[0], result)
	}

	interface Step {
		IKismetObject apply(Context c, IKismetObject object)
		Expression asExpr()
		Step borrow(Expression expr)
	}

	static class PropertyStep implements Step {
		String name

		PropertyStep(String name) {
			this.name = name
		}

		IKismetObject apply(Context c, IKismetObject object) {
			object.kismetClass().propertyGet(object, name)
		}

		String toString() { ".$name" }
		Expression asExpr() { new NameExpression(name) }
		PropertyStep borrow(Expression expr) {
			new PropertyStep(expr.toString())
		}
	}

	static class SubscriptStep implements Step {
		Expression expression

		SubscriptStep(Expression expression) {
			this.expression = expression
		}

		IKismetObject apply(Context c, IKismetObject object) {
			Kismet.model(object.kismetClass().subscriptGet(object, expression.evaluate(c)))
		}

		String toString() { ".[$expression]" }
		Expression asExpr() { expression }
		SubscriptStep borrow(Expression expr) {
			new SubscriptStep(expr)
		}
	}

	static class EnterStep implements Step {
		Expression expression

		EnterStep(Expression expression) {
			this.expression = expression
		}

		IKismetObject apply(Context c, IKismetObject object) {
			def ec = new EnterContext(c)
			ec.set('it', ec.object = object)
			expression.evaluate(ec)
		}

		String toString() { ".($expression)" }

		@InheritConstructors
		static class EnterContext extends Context {
			IKismetObject object

			IKismetObject get(String name) {
				try {
					super.get(name)
				} catch (UndefinedVariableException ignored) {
					object.kismetClass().propertyGet(object, name)
				}
			}
		}
		Expression asExpr() { expression }
		EnterStep borrow(Expression expr) {
			new EnterStep(expr)
		}
	}
}

@CompileStatic
class NameExpression extends Expression {
	String text

	NameExpression(String text) { this.text = text }

	IKismetObject evaluate(Context c) {
		c.get(text)
	}

	String repr() { text }
}

@CompileStatic
class DiveExpression extends Expression {
	Expression inner

	DiveExpression(Expression inner) {
		this.inner = inner
	}

	String repr() { "dive[$inner]" }

	IKismetObject evaluate(Context c) {
		c = c.child()
		inner.evaluate(c)
	}

	Collection<Expression> getMembers() { [inner] }
	DiveExpression join(Collection<Expression> a) { new DiveExpression(a[0]) }
}

@CompileStatic
class BlockExpression extends Expression {
	Collection<Expression> content

	String repr() {
		'{\n' + content.join('\r\n').readLines().collect('  '.&concat).join('\r\n') + '\r\n}'
	}

	BlockExpression(Collection<Expression> exprs) { content = exprs }

	IKismetObject evaluate(Context c) {
		IKismetObject a = Kismet.NULL
		for (e in content) a = e.evaluate(c)
		a
	}

	Collection<Expression> getMembers() { content }
	BlockExpression join(Collection<Expression> exprs) {
		new BlockExpression(exprs)
	}
}

@CompileStatic
class CallExpression extends Expression {
	Expression callValue
	Collection<Expression> arguments = []

	CallExpression(Collection<Expression> expressions) {
		if (null == expressions || expressions.empty) return
		setCallValue(expressions[0])
		arguments = expressions.tail()
	}

	CallExpression(Expression... exprs) {
		if (null == exprs || exprs.length == 0) return
		callValue = exprs[0]
		arguments = exprs.tail().toList()
	}

	CallExpression() {}

	String repr() { "[${members.join(', ')}]" }

	Expression getAt(int i) {
		i < 0 ? this[arguments.size() + i + 1] : i == 0 ? callValue : arguments[i - 1]
	}

	IKismetObject evaluate(Context c) {
		if (null == callValue) return Kismet.NULL
		IKismetObject obj = callValue.evaluate(c)
		if (obj.inner() instanceof KismetCallable) {
			((KismetCallable) obj.inner()).call(c, (Expression[]) arguments.toArray())
		} else {
			final arr = new IKismetObject[arguments.size()]
			for (int i = 0; i < arr.length; ++i) arr[i] = arguments[i].evaluate(c)
			obj.kismetClass().call(obj, arr)
		}
	}

	Collection<Expression> getMembers() {
		def r = new ArrayList<Expression>(1 + arguments.size())
		if (callValue != null) r.add(callValue)
		r.addAll(arguments)
		r
	}

	CallExpression join(Collection<Expression> exprs) {
		new CallExpression(exprs)
	}
}

@CompileStatic
class ConstantExpression<T> extends Expression {
	IKismetObject<T> value

	String repr() { "const($value)" }

	void setValue(T obj) {
		value = Kismet.model(obj)
	}

	IKismetObject<T> evaluate(Context c) {
		value
		//Kismet.model(value.inner())
	}
}

@CompileStatic
class NumberExpression extends ConstantExpression<Number> {
	String repr() { value.toString() }

	NumberExpression(boolean type, StringBuilder[] arr) {
		StringBuilder x = arr[0]
		boolean t = false
		if (null != arr[1]) {
			x.append((char) '.').append(arr[1]); t = true
		}
		if (null != arr[2]) {
			x.append((char) 'e').append(arr[2]); t = true
		}
		String r = x.toString()
		if (null == arr[3]) setValue(t ? new BigDecimal(r) : new BigInteger(r)) else {
			if (type) {
				if (arr[3].length() == 0) setValue(new BigDecimal(r))
				else {
					int b = Integer.valueOf(arr[3].toString())
					if (b == 1) setValue(-new BigDecimal(r))
					else if (b == 32) setValue(new Float(r))
					else if (b == 33) setValue(-new Float(r))
					else if (b == 64) setValue(new Double(r))
					else if (b == 65) setValue(-new Double(r))
					else throw new NumberFormatException("Invalid number of bits $b for explicit float")
				}
			} else {
				if (t) {
					def v = new BigDecimal(r)
					if (arr[3].length() == 0) setValue(v.toBigInteger())
					else {
						int b = Integer.valueOf(arr[3].toString())
						if (b == 1) setValue(-v.toBigInteger())
						else if (b == 8) setValue(v.byteValue())
						else if (b == 9) setValue(-v.byteValue())
						else if (b == 16) setValue(v.shortValue())
						else if (b == 17) setValue(-v.shortValue())
						else if (b == 32) setValue(v.intValue())
						else if (b == 33) setValue(-v.intValue())
						else if (b == 64) setValue(v.longValue())
						else if (b == 65) setValue(-v.longValue())
						else throw new NumberFormatException("Invalid number of bits $b for explicit integer")
					}
				} else if (arr[3].length() == 0) setValue(new BigInteger(r))
				else {
					int b = Integer.valueOf(arr[3].toString())
					if (b == 1) setValue(-new BigInteger(r))
					else if (b == 8) setValue(new Byte(r))
					else if (b == 9) setValue(-new Byte(r))
					else if (b == 16) setValue(new Short(r))
					else if (b == 17) setValue(-new Short(r))
					else if (b == 32) setValue(new Integer(r))
					else if (b == 33) setValue(-new Integer(r))
					else if (b == 64) setValue(new Long(r))
					else if (b == 65) setValue(-new Long(r))
					else throw new NumberFormatException("Invalid number of bits $b for explicit integer")
				}
			}
		}
	}

	NumberExpression(Number v) { setValue(v) }

	NumberExpression(String x) {
		Parser.NumberBuilder b = new Parser.NumberBuilder(null)
		char[] a = x.toCharArray()
		for (int i = 0; i < a.length; ++i) b.doPush((int) a[i])
		setValue b.doFinish().value.inner()
	}

	VariableIndexExpression percentize(Parser p) {
		new VariableIndexExpression(value.inner().intValue())
	}

	static class VariableIndexExpression extends Expression {
		int index

		VariableIndexExpression(int index) {
			this.index = index
		}

		@Override
		IKismetObject evaluate(Context c) {
			final x = c.@variables[index]
			if (x) x.value
			else throw new KismetEvaluationException(this, "No variable at index $index")
		}
	}
}

@CompileStatic
class StringExpression extends ConstantExpression<String> {
	String raw
	Exception exception

	String toString() { "\"${StringEscaper.escape(raw)}\"" }

	StringExpression(String v) {
		try {
			setValue(StringEscaper.unescape(raw = v))
		} catch (ex) {
			exception = ex
		}
	}

	NameExpression percentize(Parser p) {
		new NameExpression(raw)
	}

	IKismetObject<String> evaluate(Context c) {
		if (null == exception) value
		else throw exception
	}
}

@CompileStatic
class StaticExpression<T extends Expression> extends ConstantExpression<Object> {
	T expression

	String repr() { expression ? "static[$expression]($value)" : "static($value)" }

	StaticExpression(T ex = null, IKismetObject val) {
		expression = ex
		value = val
	}

	StaticExpression(T ex = null, val) {
		expression = ex
		setValue(val)
	}

	StaticExpression(T ex = null, Context c) {
		this(ex, ex.evaluate(c))
	}
}

@CompileStatic
class NoExpression extends Expression {
	static final NoExpression INSTANCE = new NoExpression()

	private NoExpression() {}

	String repr() { "noexpr" }

	IKismetObject evaluate(Context c) {
		Kismet.NULL
	}
}