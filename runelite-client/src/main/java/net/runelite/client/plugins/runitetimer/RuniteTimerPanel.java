package net.runelite.client.plugins.runitetimer;

import net.runelite.api.Client;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.shadowlabel.JShadowedLabel;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

@Singleton
public class RuniteTimerPanel extends PluginPanel
{
    @Inject
    private RuniteTimerPlugin plugin;

    @Inject
    private Client client;

    private JPanel header;
    private JPanel rowPanel;

    private ArrayList<RockProgressElement> elements = new ArrayList<RockProgressElement>();
    private Timer timer;

    public class RockProgressElement
    {
        public JLabel label;
        public JProgressBar[] bars;
        public RuniteTimerPlugin.Rock[] rocks;
        public net.runelite.api.World world;
        private Color defaultTextColor;

        public RockProgressElement(RuniteTimerPlugin.Mine someMine) {
            this.label = makeWorldLabel(someMine.world);
            this.rocks = someMine.rocks;
            this.bars = makeRockProgress(someMine.rocks);
            this.world = someMine.world;
            this.defaultTextColor = this.label.getForeground();
        }

        public void updateElem() {
            for(int i=0; i < this.rocks.length; i++) {
                RuniteTimerPlugin.Rock rock = this.rocks[i];
                JProgressBar bar = this.bars[i];

                if(this.world.getId() == client.getWorld()) {
                    this.label.setText(">W" + this.world.getId());
                    this.label.setForeground(Color.GREEN.darker());
                }
                else {
                    this.label.setText(" W" + this.world.getId());
                    this.label.setForeground(this.defaultTextColor);
                }

                if(rock.unknown) bar.setString("-");
                else {
                    int timeLeft = (int) (rock.duration.toMillis() / 1000L) - rock.timeDone();
                    if (timeLeft<0) timeLeft = -timeLeft;

                    int minutes = (timeLeft % 3600) / 60;
                    int seconds = timeLeft % 60;

                    bar.setString(String.format("%d:%02d", minutes, seconds));
                }
                bar.setForeground(Color.LIGHT_GRAY);
                bar.setBackground(rock.color());
                bar.setValue(rock.timeDone());
            }
        }
    }

    public void init() {

        this.timer = new Timer(100, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        });

        setLayout(new BorderLayout());
        this.generateHeader();
        this.generateRows();

        timer.start();
    }

    public void generateHeader() {
        JPanel newHeader = new JPanel(new BorderLayout());

        JButton resetButton = new JButton("Reset");
        resetButton.setFocusable(false);
        resetButton.addActionListener(l -> resetElems());

        JComboBox mineSelect = new JComboBox<>(plugin.getMineList());
        mineSelect.addActionListener(e -> {
            JComboBox cb = (JComboBox)e.getSource();
            String mine = (String)cb.getSelectedItem();
            plugin.changeMine(mine);
            this.generateRows();
            this.resetElems();
        });

        newHeader.add(resetButton, BorderLayout.WEST);
        newHeader.add(mineSelect, BorderLayout.CENTER);

        if (header == null) {
            this.header = newHeader;
            add(newHeader, BorderLayout.NORTH);
        }
    }

    public void generateRows() {
        JPanel newPanel = new JPanel(new GridLayout(0, 1));

        elements.clear();
        for(RuniteTimerPlugin.Mine mine : plugin.worlds.mines) {
            elements.add(new RockProgressElement(mine));
        }

        int columns = elements.size() > 0 ? elements.get(0).bars.length : 2;

        elements.forEach(e -> {
            JPanel row = new JPanel(new BorderLayout());
            JPanel erow = new JPanel(new GridLayout(0, columns));

            for(JProgressBar bar: e.bars) {
                erow.add(bar);
            }
            row.add(e.label, BorderLayout.WEST);
            row.add(erow, BorderLayout.CENTER);
            newPanel.add(row);
        });

        if (rowPanel != null) remove(rowPanel);
        rowPanel = newPanel;
        add(newPanel, BorderLayout.CENTER);
    }

    private void checkElems() {
        System.out.println("Checking elements at panel update:");
        for(int i = 0; i < elements.size(); i++) {
            RockProgressElement elem = elements.get(i);
            System.out.println("Index "+i+": Elem for "+elem.label.getText()+" with "+elem.rocks.length+" rocks");
        }
    }

    private void cleckComponents() {
        System.out.println("Checking components at panel update:");
        for(int i=0; i<rowPanel.getComponents().length;i++) {
            Component comp = (rowPanel.getComponents())[i];
            if(comp instanceof JLabel) {
                System.out.println("Index "+i+": Label for "+((JLabel) comp).getText());
            }
            if(comp instanceof JProgressBar) {
                System.out.println("Index "+i+": Progressbar with progress "+((JProgressBar) comp).getPercentComplete());
            }
        }
    }

    private JLabel makeWorldLabel(net.runelite.api.World w) {
        JLabel label = new JShadowedLabel("W" + w.getId());
        label.setMinimumSize(new Dimension(40, 0));
        label.setPreferredSize(new Dimension(40, 0));

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                plugin.sendwarn("Attempting to hop to W"+w.getId());
                plugin.hop(w);
            }
        });

        return label;
    }

    private JProgressBar[] makeRockProgress(RuniteTimerPlugin.Rock[] rocks) {
        JProgressBar[] res = new JProgressBar[rocks.length];
        for(int i = 0; i < rocks.length; i++) {
            int duration = (int) (rocks[i].duration.toMillis() / 1000L);
            JProgressBar bar = new JProgressBar(0, duration);
            bar.setStringPainted(true);
            bar.setValue(0);
            res[i] = bar;
        }
        return res;
    }

    private void resetElems() {elements.forEach(e -> {
        for(RuniteTimerPlugin.Rock rock: e.rocks) {
            rock.unknown = true;
            rock.checked = false;
        }

    });}

    public void refresh() {
        for (RockProgressElement elem : elements) {
            elem.updateElem();
        }
        //checkElems();
        //cleckComponents();
    }
}