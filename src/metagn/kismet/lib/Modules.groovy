package metagn.kismet.lib

import groovy.transform.CompileStatic
import metagn.kismet.Kismet
import metagn.kismet.call.*
import metagn.kismet.scope.KismetModule
import metagn.kismet.scope.Module
import metagn.kismet.scope.TypedContext
import metagn.kismet.type.TupleType
import metagn.kismet.type.Type
import metagn.kismet.vm.IKismetObject
import metagn.kismet.vm.Memory
import metagn.kismet.vm.RuntimeMemory

import static metagn.kismet.lib.Functions.TYPED_TEMPLATE_TYPE
import static metagn.kismet.lib.Functions.TYPE_CHECKER_TYPE

@CompileStatic
class Modules extends NativeModule {
    Modules() {
        super("modules")
        define 'current_module_path', TYPE_CHECKER_TYPE, new TypeChecker() {
            TypedExpression transform(TypedContext context, Expression... args) {
                new TypedStringExpression(context.module instanceof KismetModule &&
                    ((KismetModule) context.module).handle instanceof File ?
                    ((File) ((KismetModule) context.module).handle).path :
                    null)
            }
        }
        define 'current_module_name', TYPE_CHECKER_TYPE, new TypeChecker() {
            TypedExpression transform(TypedContext context, Expression... args) {
                new TypedStringExpression(context.module instanceof KismetModule ?
                    ((KismetModule) context.module).name :
                    null)
            }
        }
        define 'current_module_source', TYPE_CHECKER_TYPE, new TypeChecker() {
            TypedExpression transform(TypedContext context, Expression... args) {
                new TypedStringExpression(context.module instanceof KismetModule ?
                    ((KismetModule) context.module).source :
                    null)
            }
        }
        define 'module_by_name', TYPE_CHECKER_TYPE, new TypeChecker() {
            TypedExpression transform(TypedContext context, Expression... args) {
                new TypedStringExpression(context.module instanceof KismetModule ?
                    ((KismetModule) context.module).source :
                    null)
            }
        }
        define 'import', TYPED_TEMPLATE_TYPE.generic(new TupleType().withVarargs(Strings.STRING_TYPE), Type.NONE), new TypedTemplate() {
            @Override
            TypedExpression transform(TypedContext context, TypedExpression... args) {
                Module[] mods = new Module[args.length]
                int originalHeritageSize = context.heritage.size()
                for (int i = 0; i < args.length; ++i) {
                    def a = args[i]
                    def file = new File(a.instruction.evaluate(context).toString())
                    def mod = KismetModule.from(((KismetModule<File>) context.module).space, file)
                    mod.type()
                    context.heritage.add(mod.typedContext)
                    ((KismetModule<File>) context.module).dependencies.add(mod)
                    mods[i] = mod
                }
                new BasicTypedExpression(Type.NONE, new Instruction() {
                    IKismetObject evaluate(Memory m) {
                        for (int i = 0; i < mods.size(); ++i) {
                            def mod = mods[i]
                            ((RuntimeMemory) m).heritage[originalHeritageSize + i] = mod.run()
                        }
                        Kismet.NULL
                    }
                })
            }
        }
    }
}
