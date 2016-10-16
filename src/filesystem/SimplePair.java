package filesystem;

public class SimplePair<K,V>{
	private K key;
	private V val;
	
	public SimplePair(K k, V v){
		this.setKey(k);
		this.setVal(v);
	}
	public K getKey() {
		return key;
	}
	
	public void setKey(K key) {
		this.key = key;
	}
	public V getVal() {
		return val;
	}
	
	public void setVal(V val) {
		this.val = val;
	}
}