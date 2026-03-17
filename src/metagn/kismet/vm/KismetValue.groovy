package metagn.kismet.vm

import groovy.transform.CompileStatic
import metagn.kismet.call.Expression
import metagn.kismet.call.Function
import metagn.kismet.call.GroovyFunction
import metagn.kismet.call.Instruction
import metagn.kismet.call.Instructor
import metagn.kismet.call.Template
import metagn.kismet.call.TypeChecker
import metagn.kismet.call.TypedTemplate
import metagn.kismet.exceptions.UnexpectedValueException
import metagn.kismet.lib.CollectionsIterators
import metagn.kismet.lib.IteratorIterable
import metagn.kismet.lib.Logic
import metagn.kismet.lib.Reflection
import metagn.kismet.lib.Strings
import metagn.kismet.lib.Types
import metagn.kismet.type.GenericType
import metagn.kismet.type.NumberType
import metagn.kismet.type.TupleType
import metagn.kismet.type.Type
import metagn.kismet.type.TypeBound

import static metagn.kismet.lib.Functions.*

@CompileStatic
class KismetValue implements IKismetObject {
    Type type
    Object inner

    Object inner() { inner }

    KismetValue(Object value, Type t = null) {
        inner = value
        if (null != t) type = t
        else if (value instanceof Number) type = NumberType.from(value)
        else if (value instanceof Character) type = NumberType.Char
        else if (value instanceof CharSequence) type = Strings.STRING_TYPE
        else if (value instanceof Boolean) type = Logic.BOOLEAN_TYPE
        else if (value.getClass().isArray() || value instanceof Tuple)
            type = TupleType.BASE
        else if (value instanceof List) type = CollectionsIterators.LIST_TYPE
        else if (value instanceof Map) type = CollectionsIterators.MAP_TYPE
        else if (value instanceof Set) type = CollectionsIterators.SET_TYPE
        else if (value instanceof Closure) {
            inner = new GroovyFunction((Closure) value)
            type = FUNCTION_TYPE
        } else if (value instanceof Iterator) {
            if (value !instanceof Iterable)
                inner = new IteratorIterable((Iterator) value)
            type = CollectionsIterators.CLOSURE_ITERATOR_TYPE
        } else if (value instanceof Function) type = FUNCTION_TYPE//func(Type.NONE, new TupleType(new Type[0]).withVarargs(Type.ANY))
        else if (value instanceof Template) type = TEMPLATE_TYPE
        else if (value instanceof TypeChecker) type = TYPE_CHECKER_TYPE
        else if (value instanceof Instructor) type = INSTRUCTOR_TYPE
        else if (value instanceof TypedTemplate) type = TYPED_TEMPLATE_TYPE
        else if (value instanceof Type) type = new GenericType(Types.META_TYPE, value)
        else if (value instanceof Expression) type = Reflection.EXPRESSION_TYPE
        else if (value instanceof Instruction) type = Reflection.INSTRUCTION_TYPE
        else if (value instanceof Memory) type = Reflection.MEMORY_TYPE
        else if (value instanceof TypeBound) type = Types.TYPE_BOUND_TYPE
        else throw new UnexpectedValueException('give type for value ' + value + ' with class ' + value.getClass())
    }

    static KismetValue from(obj) {
        if (obj instanceof KismetValue) obj
        else new KismetValue(obj)
    }
}
