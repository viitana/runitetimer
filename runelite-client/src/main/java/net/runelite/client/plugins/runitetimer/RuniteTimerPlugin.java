package net.runelite.client.plugins.runitetimer;

import net.runelite.client.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.WorldUtil;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.http.api.worlds.WorldClient;
import net.runelite.http.api.worlds.WorldResult;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldType;

import javax.inject.Inject;
import java.awt.*;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@PluginDescriptor(
        name = "Runite rock timer",
        description = "Time runite rock spawns across various mines in Gielinor",
        tags = {"overlay", "tiles"}
)

public class RuniteTimerPlugin extends Plugin
{

    final static boolean debug = false;
    final static Set<Integer> RUNITE_ROCK = Stream.of(11376, 11377, 36209).collect(Collectors.toSet());
    final static Set<Integer> EMPTY_ROCK = Stream.of(11390, 11391, 36202).collect(Collectors.toSet());

    public java.util.List<net.runelite.api.World> worldList = new ArrayList<net.runelite.api.World>();
    private Instant lastLoad = Instant.now();
    private Boolean worldsChecked = false;
    private net.runelite.api.World hopW = null;
    public Worlds worlds = null;

    private WorldResult worldResult;

    private int[][] fossil = {{3779,3814}, {3781, 3817}};
    private int[][] fremennik = {{2375,3850}};
    private int[][] plateau = {{2948,3914}, {2964,3933}, {2976,3937}};
    private int[][] heroes = {{2937,9882}, {2941,9884}};
    private int[][] isafdar = {{2278,3156}, {2280,3160}};
    private int[][] maze = {{3059,3885}, {3060,3884}};
    private int[][] mazedungeon = {{3046,10265}};
    private int[][] guild = {{3054,9725}, {3056,9721}};
    private int[][] mourner = {{1993,4664}};
    private int[][] myth = {{1937,9020}, {1939,9019}};
    private int[][] tzhaar = {{2498,5065}, {2501,5066}, {2504,5059}};
    private int[][] priff = {{3284,12459}, {3287,12455}, {3291,12441}, {3301,12438}};

    final private Map<String, int[][]> mines =
        Stream.of(new Object[][] {
            { "Fossil Island", fossil },
            { "Fremennik Isles", fremennik },
            { "Frozen Waste Plateau", plateau },
            { "Heroes' Guild", heroes },
            { "Isafdar mine", isafdar },
            { "Lava Maze", maze },
            { "Lava Maze Dungeon", mazedungeon },
            { "Mining Guild", guild },
            { "Mourner Tunnels", mourner },
            { "Myths' Guild", myth },
            { "Mor Ul Rek", tzhaar },
            { "Trahaearn (Prifddinas)", priff },
    }).collect(Collectors.toMap(data -> (String) data[0], data -> (int[][]) data[1]));

    private static String defaultMine = "Trahaearn (Prifddinas)";
    private String currMineName = defaultMine;

    public Mine currMine(net.runelite.api.World w) {
        List<Rock> rocks = new ArrayList<Rock>();
        for (int[] pos : mines.get(currMineName)) {
            rocks.add(new Rock(pos[0], pos[1]));
        }
        Rock[] arr = rocks.toArray(new Rock[rocks.size()]);
        return new Mine(arr, w);
    }

    private Boolean fetchWorlds()
    {
        log.debug("Fetching worlds");

        try
        {
            WorldResult worldResult = new WorldClient().lookupWorlds();

            if (worldResult != null)
            {
                worldResult.getWorlds().sort(Comparator.comparingInt(net.runelite.http.api.worlds.World::getId));
                List<World> worlds = worldResult.getWorlds();

                for(World w : worlds) {
                    final net.runelite.api.World rsWorld = client.createWorld();
                    rsWorld.setActivity(w.getActivity());
                    rsWorld.setAddress(w.getAddress());
                    rsWorld.setId(w.getId());
                    rsWorld.setPlayerCount(w.getPlayers());
                    rsWorld.setLocation(w.getLocation());
                    rsWorld.setTypes(WorldUtil.toWorldTypes(w.getTypes()));
                    this.worldList.add(rsWorld);
                }

                this.worldResult = worldResult;

                EnumSet<WorldType> blacklist = EnumSet.of(WorldType.PVP, WorldType.HIGH_RISK, WorldType.DEADMAN, WorldType.SEASONAL_DEADMAN, WorldType.TOURNAMENT);
                Stream<net.runelite.api.World> worldStream = worldList.stream();

                for(int i = 0; i < worldList.size(); i++) {
                    World w = worlds.get(i);
                    EnumSet<WorldType> types = w.getTypes().clone();
                    for(WorldType t : types) {
                        if(blacklist.contains(t)) {
                            worldStream = worldStream.filter(world -> world.getId() != w.getId());
                            break;
                        }
                    }
                    if(!types.contains(WorldType.MEMBERS)) worldStream = worldStream.filter(world -> world.getId() != w.getId());
                }

                this.worldList = worldStream.collect(Collectors.toList());

                worldsChecked = true;
                //log.warn("Worldlist after filter (" + worldList.size() + "):" + worldList.stream().map(world -> world.getId()).collect(Collectors.toList()).toString());
                return true;
            }
            else return false;
        }
        catch (IOException e)
        {
            log.warn("Error looking up worlds", e);
            return false;
        }
    }

    public void sendwarn(String str) {
        log.warn(str);
    }


    public class Mine
    {
        public Rock[] rocks;
        public net.runelite.api.World world;
        public int rockCount;

        public Mine(Rock[] points, net.runelite.api.World w)
        {
            this.rocks = points;
            this.rockCount = points.length;
            this.world = w;
        }

        public Mine clone(net.runelite.api.World cw)
        {
            List<Rock> rs = new ArrayList<Rock>();
            for (Rock r : this.rocks) rs.add(r.clone());
            return new Mine((Rock[]) rs.toArray(), cw);
        }

        private void setRockAt(WorldPoint somePoint, Boolean availability) {
            for (int i = 0; i < this.rocks.length; i++) {
                if(this.rocks[i].pos.getX() == somePoint.getX() && this.rocks[i].pos.getY() == somePoint.getY()) {
                    if(availability) this.rocks[i].spawn();
                    else this.rocks[i].mineRock();
                }
            }
        }
        public Rock getRockAt(WorldPoint somePoint) {
            for (int i = 0; i < this.rocks.length; i++) {
                if(this.rocks[i].pos.getX() == somePoint.getX() && this.rocks[i].pos.getY() == somePoint.getY()) {
                    return this.rocks[i];
                }
            }
            return new Rock(0,0);
        }
    }

    public class Rock
    {
        public Point pos;
        public Boolean unknown = true;
        public Boolean checked = false;
        private Boolean status = false;
        private int spawnTime = 720;

        private Instant startTime;
        public final Duration duration;

        public Rock(int x, int y) {
            this.pos = new Point(x,y);
            this.startTime = Instant.now();
            this.duration = Duration.ofSeconds(spawnTime);
        }

        public Rock clone() {
            return new Rock(this.pos.getX(), this.pos.getY());
        }


        public void mineRock() {
            this.status = false;
            this.startTime = Instant.now();
            this.unknown = false;
            this.checked = true;
        }

        public void spawn() {
            this.status = true;
            this.unknown = false;
            this.checked = true;
        }

        public int timeDone() {
            if(this.unknown) return 0;
            if(this.status) return this.spawnTime;
            int done = (int) (Duration.between(this.startTime, Instant.now()).abs().toMillis()/1000L);
            done = Math.min(done, spawnTime);
            if(done == spawnTime) this.status = true;
            return done;
        }
        public Color color() {
            if(this.checked && !this.status && this.unknown) return Color.GRAY;
            if(this.unknown) return Color.GRAY.darker();
            if(this.status) return Color.GREEN.darker();
            if(this.timeDone() > spawnTime-60) return Color.YELLOW.darker();
            return Color.ORANGE.darker();
        }

        public Color textColor() {
            if(unknown) return Color.LIGHT_GRAY;

            float R = 255F;
            float G = 255F;
            float B = 10F;
            float done = (float) (timeDone());
            float total = (float) (spawnTime);

            if(done < total/3) G = G - ((((total/3)-done)/(total/3))*63);
            else R = R - ((done-(total/3))/((total*2)/3))*120;

            int r = (int) (R);
            int g = (int) (G);
            int b = (int) (B);

            return new Color(r,g,b);
        }

        public Color barColor() {
            if(unknown) return Color.GRAY.darker();

            float R = 255F;
            float G = 255F;
            float B = 10F;
            float done = (float) (timeDone());
            float total = (float) (spawnTime);
            float mult = 0.3F;

            if(done < total/3) G = (G - ((((total/3)-done)/(total/3)))*63)*mult;
            else R = (R - ((done-(total/3))/((total*2)/3))*120)*mult;

            int r = (int) (R);
            int g = (int) (G);
            int b = (int) (B);

            return new Color(r,g,b);
        }
    }

    public class Worlds
    {
        public Mine[] mines;

        public Worlds() {
            this.mines = new Mine[worldList.size()];
            for (int i = 0; i < worldList.size(); i++) {
                this.mines[i] = currMine(worldList.get(i));
            }
        }

        public Mine w(int n) {
            return mines[n];
        }
    }

    private NavigationButton navigationButton;

    @Inject
    private Client client;

    @Inject
    SkillIconManager iconManager;

    @Inject
    private ClientToolbar pluginToolbar;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private RuniteTimerOverlay overlay;

    public Mine currentMine() {
        for (Mine mine : worlds.mines) {
            if(mine.world.getId() == client.getWorld()) {
                return mine;
            }
        }
        return currMine(null);
    }

    private boolean loggingIn() {
        GameState state = client.getGameState();
        return (state == GameState.LOGGING_IN
                | state == GameState.HOPPING
                | state == GameState.LOGIN_SCREEN
                | state == GameState.LOADING);
    }

    private boolean wasLoading() {
        return (Duration.between(lastLoad, Instant.now()).abs().toMillis() < 1000);
    }

    RuniteTimerPanel pluginPanel;

    Boolean wasMining = false;
    private Set<Integer> miningAnims = new HashSet<Integer>( Arrays.asList(
            AnimationID.MINING_BRONZE_PICKAXE,
            AnimationID.MINING_IRON_PICKAXE,
            AnimationID.MINING_STEEL_PICKAXE,
            AnimationID.MINING_BLACK_PICKAXE,
            AnimationID.MINING_MITHRIL_PICKAXE,
            AnimationID.MINING_ADAMANT_PICKAXE,
            AnimationID.MINING_RUNE_PICKAXE,
            AnimationID.MINING_DRAGON_PICKAXE,
            AnimationID.MINING_DRAGON_PICKAXE_OR,
            AnimationID.MINING_DRAGON_PICKAXE_UPGRADED,
            AnimationID.MINING_3A_PICKAXE,
            AnimationID.MINING_CRYSTAL_PICKAXE));

    @Subscribe
    private void onAnimationChanged(AnimationChanged anim)
    {
        // We started mining
        if (anim.getActor() == client.getLocalPlayer() && miningAnims.contains(anim.getActor().getAnimation()))
        {
            wasMining = true;
        }
        // We stopped mining
        if (anim.getActor() == client.getLocalPlayer() && anim.getActor().getAnimation() == AnimationID.IDLE && wasMining)
        {
            wasMining = false;
        }
    }
    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        GameObject gameObject = event.getGameObject();
        if (RUNITE_ROCK.contains(gameObject.getId()))
        {
            WorldPoint pos = gameObject.getWorldLocation();
            if(!wasLoading()) currentMine().setRockAt(pos, true);
        }

        if (EMPTY_ROCK.contains(gameObject.getId()))
        {
            WorldPoint pos = gameObject.getWorldLocation();
            if(!wasLoading()) currentMine().setRockAt(pos, false);
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        Mine mine = currentMine();
        Scene scene = client.getScene();
        Tile[][][] tiles = scene.getTiles();
        int z = client.getPlane();

        for (int x = 0; x < Constants.SCENE_SIZE; ++x) {
            for (int y = 0; y < Constants.SCENE_SIZE; ++y) {
                Tile tile = tiles[z][x][y];
                if (tile == null)
                {
                    continue;
                }
                Player player = client.getLocalPlayer();
                if (player == null)
                {
                    continue;
                }

                GameObject[] objs = tile.getGameObjects();
                if (objs != null) {
                    //System.out.println(objs.length);
                    for(GameObject obj : objs) {

                        if(obj != null) {
                            //System.out.println(obj.getId());
                            Rock rock = mine.getRockAt(obj.getWorldLocation());
                            if (RUNITE_ROCK.contains(obj.getId())) {
                                rock.checked = true;
                                WorldPoint pos = obj.getWorldLocation();
                                //log.warn("Runite at " + pos.toString());
                                mine.setRockAt(pos, true);
                            } else if (EMPTY_ROCK.contains(obj.getId())) {
                                WorldPoint pos = obj.getWorldLocation();
                                //log.warn("Empty at " + pos.toString());
                                rock.checked = true;
                                if (rock.status) {
                                    rock.status = false;
                                    rock.unknown = true;
                                }
                            }
                        }
                    }
                }

            }
        }


        if(hopW != null) {
            log.warn("Attempting to hop: W"+ hopW.getId());
            client.hopToWorld(hopW);
            hopW = null;
        }

    }

    public void changeMine(String s) {
        currMineName = s;
        worlds = new Worlds();
    }

    public String[] getMineList() {
        Set<String> keys = this.mines.keySet();
        String[] k = keys.toArray(new String[keys.size()]);
        Arrays.sort(k);
        return k;
    }

    public void hop(net.runelite.api.World w)
    {
        if (client.getGameState() == GameState.LOGIN_SCREEN)
        {
            // on the login screen we can just change the world by ourselves
            client.changeWorld(w);
            return;
        }
        hopW = w;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        GameState state = event.getGameState();
        if(state == GameState.LOGGING_IN
                | state == GameState.HOPPING
                | state == GameState.LOGIN_SCREEN
                | state == GameState.LOADING) {
            lastLoad = Instant.now();
        }
    }

    @Override
    protected void startUp() throws Exception
    {
        fetchWorlds();
        worlds = new Worlds();
        pluginPanel = injector.getInstance(RuniteTimerPanel.class);
        pluginPanel.init();

        overlayManager.add(overlay);
        //overlayManager.add(minimapOverlay);

        navigationButton = NavigationButton.builder()
                .tooltip("Runite rock timer")
                .icon(iconManager.getSkillImage(Skill.MINING))
                .panel(pluginPanel)
                .build();

        pluginToolbar.addNavigation(navigationButton);

    }
}
