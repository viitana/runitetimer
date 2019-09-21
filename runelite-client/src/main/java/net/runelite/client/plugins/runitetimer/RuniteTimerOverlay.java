package net.runelite.client.plugins.runitetimer;

import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;
import java.util.Random;

import javax.inject.Inject;
import java.awt.*;

public class RuniteTimerOverlay extends Overlay
{
    private static final int MAX_DRAW_DISTANCE = 32;
    private static final int TILE_WIDTH = 4;
    private static final int TILE_HEIGHT = 4;

    private final Client client;
    private final RuniteTimerPlugin plugin;

    @Inject
    private RuniteTimerOverlay(Client client, RuniteTimerPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.LOW);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        Scene scene = client.getScene();
        Tile[][][] tiles = scene.getTiles();
        int z = client.getPlane();

        for (int x = 0; x < Constants.SCENE_SIZE; ++x) {
            for (int y = 0; y < Constants.SCENE_SIZE; ++y) {
                Tile tile = tiles[z][x][y];
                if (tile == null) continue;

                Player player = client.getLocalPlayer();
                if (player == null) continue;

                WorldPoint tilePos = tile.getWorldLocation();

                RuniteTimerPlugin.Mine mine = plugin.currentMine();
                for (RuniteTimerPlugin.Rock r : mine.rocks) {
                    Point rockPos = r.pos;
                    if (rockPos.getX() == tilePos.getX() && rockPos.getY() == tilePos.getY()) {
                        drawTile(graphics, tilePos, r.color());
                        drawOnMinimap(graphics, tilePos, r.color());
                    }
                }
            }
        }
        return null;
    }

    private void drawTile(Graphics2D graphics, WorldPoint point, Color color)
    {
        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();

        if (point.distanceTo(playerLocation) >= MAX_DRAW_DISTANCE) return;

        LocalPoint lp = LocalPoint.fromWorld(client, point);
        if (lp == null) return;

        Polygon poly = Perspective.getCanvasTilePoly(client, lp);
        if (poly == null) return;

        OverlayUtil.renderPolygon(graphics, poly, color);
    }

    private void drawOnMinimap(Graphics2D graphics, WorldPoint point, Color color)
    {
        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();

        if (point.distanceTo(playerLocation) >= MAX_DRAW_DISTANCE) return;

        LocalPoint lp = LocalPoint.fromWorld(client, point);
        if (lp == null) return;

        Point posOnMinimap = Perspective.localToMinimap(client, lp);
        if (posOnMinimap == null) return;

        OverlayUtil.renderMinimapRect(client, graphics, posOnMinimap, TILE_WIDTH, TILE_HEIGHT, color);
        OverlayUtil.renderMinimapRect(client, graphics, posOnMinimap, TILE_WIDTH - 1, TILE_HEIGHT - 1, color);
        OverlayUtil.renderMinimapRect(client, graphics, posOnMinimap, TILE_WIDTH + 1, TILE_HEIGHT + 1, color);
    }

}


