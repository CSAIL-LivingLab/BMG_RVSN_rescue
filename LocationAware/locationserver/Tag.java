package locationserver;

/** Definition of a virtual tag.
 */

public class Tag implements java.io.Serializable {
    private String name;
    /** construct a new tag with a unique name.  Giving an already
     * used name will have undefined results */
    public Tag(String name) {
	this.name = name;
    }
}
