import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lux.*;

public class Bot {
    private static int round = 0;
    public static void main(final String[] args) throws Exception {
        FileWriter fileWriter = new FileWriter("log.txt");
        PrintWriter printWriter = new PrintWriter(fileWriter);
        Agent agent = new Agent();
        Cell build_location = null;
        // initialize
        agent.initialize();
        while (agent.gameState.turn <=360) {
            /** Do not edit! **/
            // wait for updates
            agent.update();

            ArrayList<String> actions = new ArrayList<>();
            GameState gameState = agent.gameState;
            /** AI Code Goes Below! **/

            Player player = gameState.players[gameState.id];
            Player opponent = gameState.players[(gameState.id + 1) % 2];
            GameMap gameMap = gameState.map;
            final List<CityTile> cityTiles = player.cities.values().stream().flatMap(city -> city.citytiles.stream()).collect(Collectors.toList());
            ArrayList<Cell> resourceTiles = getResourceTiles(gameMap);
            boolean build_city = (double) player.units.size() / (double) cityTiles.size() >= 0.75;

            // we iterate over all our units and do something with them
            for (Unit unit : player.units) {
                if (unit.isWorker() && unit.canAct()) {
                    printWriter.println("We can Act");
                    if (unit.getCargoSpaceLeft() > 0) {
                        // if the unit is a worker and we have space in cargo, lets find the nearest resource tile and try to mine it
                        printWriter.println("Try to Mine");
                        Cell closestResourceTile = getClosestResourceTile(player, resourceTiles, unit);

                        if (closestResourceTile != null) {
                            Direction dir = unit.pos.directionTo(closestResourceTile.pos);
                            // move the unit in the direction towards the closest resource tile's position.
                            actions.add(unit.move(dir));
                        }
                    } else {
                        // if unit is a worker and there is no cargo space left, and we have cities, lets return to them
                        if (build_city) {
                            if (build_location == null){
                                CityTile emptyNearCity = getClosestCityTile(unit, player);
                                Direction[] checkDirections = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, };
                                for (Direction dir : checkDirections) {
                                    try{
                                        final Cell possibleEmptyTile = gameState.map.getCellByPos(emptyNearCity.pos.translate(dir, 1));
                                        if(!possibleEmptyTile.hasResource() && possibleEmptyTile.road == 0 && possibleEmptyTile.citytile == null) {
                                            build_location = possibleEmptyTile;
                                            printWriter.println("Build City here " + build_location.pos.toString());
                                            break;
                                        }
                                    }catch (Exception ignored){ }
                                }
                            } else if(unit.pos.equals(build_location.pos)){
                                printWriter.println("Build City");
                                actions.add(unit.buildCity());
                                build_city = false;
                                build_location = null;
                                continue;
                            } else {
                                printWriter.println("Move to Location " + build_location.pos.toString());
                                Position posDiff = new Position(build_location.pos.x - unit.pos.x, build_location.pos.y - unit.pos.y);
                                int xdiff = posDiff.x;
                                int ydiff = posDiff.y;

                                if (Math.abs(ydiff) > Math.abs(xdiff)) {
                                    Cell checkTile = gameState.map.getCell(unit.pos.x, unit.pos.y+(ydiff > 0 ? 1 : -1));
                                    printWriter.println("Move to Location " + checkTile.pos.toString());
                                    if (checkTile.citytile == null) {
                                        if (ydiff > 0){
                                            actions.add(unit.move(Direction.SOUTH));
                                        } else {
                                            actions.add(unit.move(Direction.NORTH));
                                        }
                                    }else{
                                        if (xdiff > 0){
                                            actions.add(unit.move(Direction.EAST));
                                        } else {
                                            actions.add(unit.move(Direction.WEST));
                                        }
                                    }
                                }else{
                                    Cell checkTile = gameState.map.getCell(unit.pos.x+(xdiff > 0 ? 1 : -1), unit.pos.y);
                                    printWriter.println("Move to Location " + checkTile.pos.toString());
                                    if (checkTile.citytile == null) {
                                        if (xdiff > 0){
                                            actions.add(unit.move(Direction.EAST));
                                        } else {
                                            actions.add(unit.move(Direction.WEST));
                                        }
                                    }else {
                                        if (ydiff > 0) {
                                            actions.add(unit.move(Direction.SOUTH));
                                        } else {
                                            actions.add(unit.move(Direction.NORTH));
                                        }
                                    }
                                }
                                continue;
                            }
                        }
                        actions.add(Annotate.sidetext("Try to go Home"));
                        if (player.cities.size() > 0) {
                            CityTile closestCityTile = getClosestCityTile(unit, player);
                            if (closestCityTile != null) {
                                Direction dir = unit.pos.directionTo(closestCityTile.pos);
                                actions.add(unit.move(dir));
                            }
                        }
                    }
                }
            }
            for (CityTile cityTile : cityTiles){
                if(cityTile.canAct() && cityTiles.size() > player.units.size()){
                    actions.add(cityTile.buildWorker());
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
        printWriter.close();
    }

    private static ArrayList<Cell> getResourceTiles(GameMap gameMap) {
        ArrayList<Cell> resourceTiles = new ArrayList<>();
        for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
                Cell cell = gameMap.getCell(x, y);
                if (cell.hasResource()) {
                    resourceTiles.add(cell);
                }
            }
        }
        return resourceTiles;
    }

    private static CityTile getClosestCityTile(Unit unit, Player player) {
        City city = player.cities.values().iterator().next();
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
