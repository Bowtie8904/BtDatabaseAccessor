package core.app;

import bt.gui.fx.core.FxViewManager;
import core.app.views.SelectionView;

/**
 * @author &#8904
 *
 */
public class ViewManager extends FxViewManager
{
    public ViewManager()
    {
    }

    /**
     * @see bt.gui.fx.core.FxController#loadViews()
     */
    @Override
    protected void loadViews()
    {
    }

    /**
     * @see bt.gui.fx.core.FxController#startApplication()
     */
    @Override
    protected void startApplication()
    {
        setView(SelectionView.class);
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}