package hollow.knight.gui;

import java.util.ArrayList;
import java.util.List;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import hollow.knight.logic.SynchronizedEntityManager;

public final class SceneSelectorListModel implements ListModel<String> {

  private final SynchronizedEntityManager<ListDataListener> listeners =
      new SynchronizedEntityManager<>();

  private final TransitionData transitionData;
  private final TransitionVisualizerPlacements placements;
  private final List<String> scenes = new ArrayList<>();

  public SceneSelectorListModel(TransitionData transitionData,
      TransitionVisualizerPlacements placements) {
    this.transitionData = transitionData;
    this.placements = placements;
  }

  public void updateScenes(List<String> newScenes) {
    int oldSize = scenes.size();
    scenes.clear();
    scenes.addAll(newScenes);
    int newSize = scenes.size();

    ListDataEvent e =
        new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, Math.max(oldSize, newSize));
    listeners.forEach(l -> l.contentsChanged(e));
  }

  public String getScene(int index) {
    return scenes.get(index);
  }

  @Override
  public void addListDataListener(ListDataListener listener) {
    listeners.add(listener);
  }

  @Override
  public String getElementAt(int index) {
    String scene = scenes.get(index);
    return transitionData.sceneData(scene).alias() + " ("
        + placements.placementsForScene(scene).count() + ")";
  }

  @Override
  public int getSize() {
    return scenes.size();
  }

  @Override
  public void removeListDataListener(ListDataListener listener) {
    listeners.remove(listener);
  }

}
