package hlaaftana.kismet.type

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import hlaaftana.kismet.vm.IKismetObject

@CompileStatic
@EqualsAndHashCode
class UnionType extends AbstractType {
	List<Type> members

	UnionType(List<Type> members) {
		this.members = members
	}

	UnionType(Type... members) {
		this.members = members.toList()
	}

	boolean check(IKismetObject obj) {
		for (t in members) if (t.check(obj)) return true
		false
	}

	UnionType flatten() {
		def newMems = new ArrayList<Type>()
		for (m in members) {
			if (m instanceof UnionType) newMems.addAll(m.flatten())
			else newMems.add(m)
		}
		new UnionType(newMems)
	}

	Type reduced() {
		def flattened = flatten()
		def newMems = new ArrayList<Type>()
		outer: for (mem in flattened.members) {
			for (int i = 0; i < newMems.size(); ++i) {
				final newMem = newMems.get(i)
				def rel = mem.relation(newMem)
				if (rel.assignableTo) {
					break outer
				} else if (rel.super) {
					newMems.remove(i)
					--i
				}
			}
			if (mem instanceof UnionType) mem = ((UnionType) mem).reduced()
			mem instanceof UnionType ? newMems.addAll(mem.members) : newMems.add(mem)
		}
		newMems.size() == 0 ? NONE : newMems.size() == 1 ? newMems.get(0) :
			new UnionType(newMems)
	}

	String toString() {
		def res = new StringBuilder("Union[")
		for (int i = 0; i < members.size(); ++i) {
			if (i != 0) res.append(', ')
			res.append(members[i].toString())
		}
		res.append((char) ']').toString()
	}

	TypeRelation weakRelation(Type other) {
		if (other instanceof UnionType) {
			final inter = members.intersect(other.members)
			final ours = members - inter, theirs = other.members - inter
			if (ours.empty) return theirs.empty ? TypeRelation.equal() : TypeRelation.subtype(theirs.size())
			else if (theirs.empty) return TypeRelation.supertype(ours.size())
			else {
				final iter = members.iterator()
				def min = iter.next().relation((Type) other)
				while (iter.hasNext()) {
					final rel = iter.next().relation((Type) other)
					if (rel.none) return rel
					else if (!rel.equal && rel.toSome() < min.toSome()) min = rel
				}
				min
			}
		} else if (members.size() == 0) TypeRelation.subtype(Integer.MAX_VALUE)
		else {
			final iter = members.iterator()
			def f = iter.next()
			def max = f.relation(other)
			while (iter.hasNext()) {
				def a = iter.next()
				final rel = a.relation(other)
				if (max.none || (!rel.none && (rel.equal || rel.toSome() > max.toSome()))) max = rel
			}
			max
		}
	}

	int size() { members.size() }
}
