package info.teksol.mindcode.ast;


import java.util.Objects;

public class HeapAllocation extends BaseAstNode {
    private final String name;
    private final Range range;

    HeapAllocation(String name, Range range) {
        super(range);
        this.name = name;
        this.range = range;
    }

    public HeapAllocation(String name, int first, int last) {
        this(name, new InclusiveRange(new NumericLiteral(first), new NumericLiteral(last)));
    }

    public String getName() {
        return name;
    }

    public Range getRange() {
        return range;
    }

    public boolean hasRange() {
        return range != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HeapAllocation that = (HeapAllocation) o;
        return Objects.equals(name, that.name)
                && Objects.equals(range, that.range);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, range);
    }

    @Override
    public String toString() {
        return "HeapAllocation{" +
                "name='" + name + '\'' +
                ", range=" + range +
                '}';
    }
}
