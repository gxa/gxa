package ae3.restresult;

import java.io.IOException;

/**
 * @author pashky
 */
public interface RestResultRenderer {
    void render(Object object, Appendable where, final Class profile) throws RenderException, IOException;
}
