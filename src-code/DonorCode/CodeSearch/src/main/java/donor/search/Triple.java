package donor.search;

public class Triple <T1, T2, T3> {
	private T1 first;
	private T2 second;
	private T3 third;
	
	public Triple() {
	}

	public Triple(T1 fst, T2 snd, T3 trd){
		this.first = fst;
		this.second = snd;
		this.third = trd;
	}
	
	public T1 getFirst(){
		return this.first;
	}
	
	public T2 getSecond(){
		return this.second;
	}
	
	public T3 getThird(){
		return this.third;
	}
	
	public void setFirst(T1 fst){
		this.first = fst;
	}
	
	public void setSecond(T2 snd){
		this.second = snd;
	}
	
	public void setThird(T3 trd){
		this.third = trd;
	}
	
	@Override
	public String toString() {
		return "<" + first.toString() + "," + second.toString() + ',' +
				third.toString() + ">";
	}
}
