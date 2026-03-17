package metagn.kismet.lib

import groovy.transform.CompileStatic
import metagn.kismet.call.Expression
import metagn.kismet.call.Function
import metagn.kismet.call.Instructor
import metagn.kismet.call.Template
import metagn.kismet.parser.Parser
import metagn.kismet.scope.Context
import metagn.kismet.scope.Module
import metagn.kismet.scope.TypedContext
import metagn.kismet.type.GenericType
import metagn.kismet.type.SingleType
import metagn.kismet.type.Type
import metagn.kismet.vm.IKismetObject
import metagn.kismet.vm.Memory

import static metagn.kismet.call.ExprBuilder.*
import static metagn.kismet.lib.Types.inferType
import static Functions.*

@CompileStatic
class NativeModule extends Module {
    String name
    TypedContext typedContext
    Context defaultContext

    NativeModule(String name) {
        this.name = name
        typedContext = new TypedContext(name)
        typedContext.module = this
        defaultContext = new Context()
    }

    TypedContext typeContext() {
        typedContext
    }

    Memory run() {
        typedContext
    }

    void define(SingleType type) {
        define(type.name, new GenericType(Types.META_TYPE, type), type)
    }

    void define(String name, Type type, IKismetObject object) {
        typed(name, type, object)
        defaultContext.add(name, object)
    }

    void define(String name, IKismetObject object) {
        define(name, inferType(object), object)
    }

    void typed(String name, Type type, IKismetObject object) {
        if (object instanceof Function && null == object.name) ((Function) object).name = name
        if (object instanceof Function && type instanceof GenericType && type.base == FUNCTION_TYPE) {
            object.argumentTypes = type.arguments[0]
            object.returnType = type.arguments[1]
        } else if (object instanceof Instructor && type instanceof GenericType && type.base == INSTRUCTOR_TYPE) {
            object.argumentTypes = type.arguments[0]
            object.returnType = type.arguments[1]
        }
        typedContext.addVariable(name, object, type)
    }

    void alias(String old, String... news) {
        final dyn = defaultContext.get(old)
        final typ = typedContext.getAll(old)
        for (final n : news) {
            defaultContext.add(n, dyn)
            for (final t : typ) {
                typedContext.addVariable(n, t.variable.value, t.variable.type)
            }
        }
    }

    void negated(final String old, String... news) {
        def temp = new Template() {
            @Override
            Expression transform(Parser parser, Expression... args) {
                def arr = new ArrayList<Expression>(args.length + 1)
                arr.add(name(old))
                arr.addAll(args)
                call(name('not'), call(arr))
            }
        }
        for (final n : news) {
            defaultContext.add(n, temp)
            typedContext.addVariable(n, temp, TEMPLATE_TYPE)
        }
    }
}
