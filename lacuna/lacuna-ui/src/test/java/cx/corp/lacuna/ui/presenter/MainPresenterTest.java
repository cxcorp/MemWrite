package cx.corp.lacuna.ui.presenter;

import cx.corp.lacuna.core.domain.NativeProcess;
import cx.corp.lacuna.core.domain.NativeProcessImpl;
import cx.corp.lacuna.ui.model.MainModel;
import cx.corp.lacuna.ui.model.MainModelImpl;
import cx.corp.lacuna.ui.view.MainView;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class MainPresenterTest {

    private MainPresenter presenter;
    private MainModel model;
    @Mock
    private MainView view;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        model = new MainModelImpl();
        presenter = new MainPresenter(view, model);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ctorThrowsIfNullViewPassed() {
        new MainPresenter(null, model);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ctorThrowsIfNullModelPAssed() {
        new MainPresenter(view, null);
    }

    @Test
    public void initializeAttachesPresenterToView() {
        presenter.initialize();
        verify(view).attach(presenter);
    }

    @Test
    public void newActiveProcessSelectedUpdatesModel() {
        NativeProcess newProcess = new NativeProcessImpl(123, "ayy", "lmao");
        model.setActiveProcess(null);
        presenter.newActiveProcessSelected(newProcess);
        assertEquals(newProcess, model.getActiveProcess());
    }

    @Test
    public void newActiveProcessSelectedUpdatesView() {
        NativeProcess newProcess = new NativeProcessImpl(123, "ayy", "lmao");
        presenter.newActiveProcessSelected(newProcess);
        verify(view).setActiveProcess(newProcess);
    }
}