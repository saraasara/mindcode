package info.teksol.mindcode.ast;

import java.util.List;
import java.util.Objects;

public class FunctionDeclaration extends BaseAstNode {
    private final boolean inline;
    private final String name;
    private final List<VarRef> params;
    private final AstNode body;

    public FunctionDeclaration(boolean inline, String name, List<VarRef> params, AstNode body) {
        super(body);
        this.inline = inline;
        this.name = name;
        this.params = params;
        this.body = body;
    }

    public boolean isInline() {
        return inline;
    }

    public String getName() {
        return name;
    }

    public List<VarRef> getParams() {
        return params;
    }

    public AstNode getBody() {
        return body;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FunctionDeclaration that = (FunctionDeclaration) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(params, that.params) &&
                Objects.equals(body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, params, body);
    }

    @Override
    public String toString() {
        return "FunctionDeclaration{" +
                "name='" + name + '\'' +
                ", params=" + params +
                ", body=" + body +
                '}';
    }
}
