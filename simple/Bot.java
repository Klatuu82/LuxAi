import java.util.ArrayList;

import lux.*;

public class Bot {
  private static int round = 0;
  public static void main(final String[] args) throws Exception {
    Agent agent = new Agent();
    // initialize
    agent.initialize();
    while (true) {
      /** Do not edit! **/
      // wait for updates
      agent.update();

      ArrayList<String> actions = new ArrayList<>();
      GameState gameState = agent.gameState;
      /** AI Code Goes Below! **/

      Player player = gameState.players[gameState.id];
      Player opponent = gameState.players[(gameState.id + 1) % 2];
      GameMap gameMap = gameState.map;

      ArrayList<Cell> resourceTiles = new ArrayList<>();
      for (int y = 0; y < gameMap.height; y++) {
        for (int x = 0; x < gameMap.width; x++) {
          Cell cell = gameMap.getCell(x, y);
          if (cell.hasResource()) {
            resourceTiles.add(cell);
          }
        }
      }
      
      // we iterate over all our units and do something with them
      for (int i = 0; i < player.units.size(); i++) {
        Unit unit = player.units.get(i);
        if (unit.isWorker() && unit.canAct()) {
          actions.add(Annotate.sidetext("We can Act"));
          if (unit.getCargoSpaceLeft() > 0) {
            // if the unit is a worker and we have space in cargo, lets find the nearest resource tile and try to mine it
            actions.add(Annotate.sidetext("Try to Mine"));
            Cell closestResourceTile = getClosestResourceTile(player, resourceTiles, unit);

            if (closestResourceTile != null) {
              Direction dir = unit.pos.directionTo(closestResourceTile.pos);
              // move the unit in the direction towards the closest resource tile's position.
              actions.add(unit.move(dir));
            }
          } else {
            // if unit is a worker and there is no cargo space left, and we have cities, lets return to them
            actions.add(Annotate.sidetext("Try to go Home"));
            if (player.cities.size() > 0) {
              City city = player.cities.values().iterator().next();
              CityTile closestCityTile = getClosestCityTile(unit, city);
              if (closestCityTile != null) {
                Direction dir = unit.pos.directionTo(closestCityTile.pos);
                actions.add(unit.move(dir));
              }
            }
          }
          City city = player.cities.values().stream().filter(town -> town.fuel > 100).findFirst().orElse(null);
          if (city != null) {

          }
        }
      }

      // you can add debug annotations using the static methods of the Annotate class.
      // actions.add(Annotate.circle(0, 0));

      /** AI Code Goes Above! **/

      /** Do not edit! **/
      StringBuilder commandBuilder = new StringBuilder("");
      for (int i = 0; i < actions.size(); i++) {
        if (i != 0) {
          commandBuilder.append(",");
        }
        commandBuilder.append(actions.get(i));
      }
      System.out.println(commandBuilder.toString());
      // end turn
      agent.endTurn();

    }
  }

  private static CityTile getClosestCityTile(Unit unit, City city) {
    double closestDist = 999999;
    CityTile closestCityTile = null;
    for (CityTile citytile : city.citytiles) {
      double dist = citytile.pos.distanceTo(unit.pos);
      if (dist < closestDist) {
        closestCityTile = citytile;
        closestDist = dist;
      }
    }
    return closestCityTile;
  }

  private static Cell getClosestResourceTile(Player player, ArrayList<Cell> resourceTiles, Unit unit) {
    Cell closestResourceTile = null;
    double closestDist = 9999999;
    for (Cell cell : resourceTiles) {

      if (cell.resource.type.equals(GameConstants.RESOURCE_TYPES.COAL) && !player.researchedCoal()) continue;
      if (cell.resource.type.equals(GameConstants.RESOURCE_TYPES.URANIUM) && !player.researchedUranium()) continue;
      double dist = cell.pos.distanceTo(unit.pos);
      if (dist < closestDist) {
        closestDist = dist;
        closestResourceTile = cell;
      }
    }
    return closestResourceTile;
  }
}
