package hlaaftana.kismet.type

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import hlaaftana.kismet.vm.IKismetObject
import hlaaftana.kismet.vm.KismetTuple

@CompileStatic
@EqualsAndHashCode
class TupleType extends GenericType {
	static final SingleType BASE = new SingleType('Tuple') {
		boolean check(IKismetObject obj) { obj instanceof KismetTuple }
	}
	Type varargs

	TupleType() { super(BASE) }

	TupleType(Type[] bounds) {
		super(BASE, bounds)
	}

	String toString() { "Tuple[${arguments.join(', ')}" + (null == varargs ? "]" : ", varargs $varargs]") }

	boolean isIndefinite() { null != varargs }
	int size() { null == varargs ? super.size() : super.size() + 1 }

	boolean check(IKismetObject obj) {
		obj instanceof KismetTuple && check(((KismetTuple) obj).inner)
	}

	boolean check(IKismetObject... values) {
		if (null == arguments) true
		else if (null == varargs ? values.length == arguments.length : values.length >= arguments.length) {
			for (int i = 0; i < values.length; ++i) {
				if (!getAt(i).check(values[i])) return false
			}
			true
		} else false
	}

	TypeBound.Variance varianceAt(int i) {
		TypeBound.Variance.COVARIANT
	}

	TupleType withVarargs(Type varargs) {
		this.varargs = varargs
		this
	}

	Type getAt(int i) {
		i >= arguments.length ? varargs : arguments[i]
	}
}
