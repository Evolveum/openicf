package org.identityconnectors.oracle;

final class Pair<F,S> {
	private final F first;
	private final S second;
	
	/**
	 * @param first
	 * @param second
	 */
	public Pair(F first, S second) {
		super();
		this.first = first;
		this.second = second;
	}
	/**
	 * @return the first
	 */
	public F getFirst() {
		return first;
	}
	/**
	 * @return the second
	 */
	public S getSecond() {
		return second;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pair<?,?> other = (Pair<?,?>) obj;
		if (first == null) {
			if (other.first != null)
				return false;
		} else if (!first.equals(other.first))
			return false;
		if (second == null) {
			if (other.second != null)
				return false;
		} else if (!second.equals(other.second))
			return false;
		return true;
	}
	
	
}
