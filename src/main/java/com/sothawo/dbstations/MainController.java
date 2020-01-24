/*
 Copyright 2020 Peter-Josef Meisch (pj.meisch@sothawo.com)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.sothawo.dbstations;

import com.sothawo.mapjfx.Coordinate;
import com.sothawo.mapjfx.MapType;
import com.sothawo.mapjfx.MapView;
import javafx.fxml.FXML;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author P.J. Meisch (pj.meisch@sothawo.com)
 */
@Component
@FxmlView("/MainController.fxml") // if / is omiited, resource loader expects the fxml in the package
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    private static final int ZOOM_DEFAULT = 14;
    private static final Coordinate coordKarlsruheCastle = new Coordinate(49.013517, 8.404435);

    private final DBStationsConfiguration configuration;

    @FXML
    private MapView mapViewLeft;

    @FXML
    private MapView mapViewRight;

    public MainController(DBStationsConfiguration configuration) {
        this.configuration = configuration;
    }

    @FXML
    public void initialize() {
        init(mapViewLeft);
        init(mapViewRight);
    }

    private void init(MapView mapView) {
        mapView.initializedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                mapView.setZoom(ZOOM_DEFAULT);
                mapView.setCenter(coordKarlsruheCastle);
            }
        });
        if (configuration.getBingMapsApiKey() != null) {
            mapView.setBingMapsApiKey(configuration.getBingMapsApiKey());
            mapView.setMapType(MapType.BINGMAPS_ROAD);
        } else {
            mapView.setMapType(MapType.OSM);
        }
        mapView.initialize();
    }
}
