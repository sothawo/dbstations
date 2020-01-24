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
import com.sothawo.mapjfx.Extent;
import com.sothawo.mapjfx.MapLabel;
import com.sothawo.mapjfx.MapType;
import com.sothawo.mapjfx.MapView;
import com.sothawo.mapjfx.Marker;
import com.sothawo.mapjfx.event.MapViewEvent;
import javafx.fxml.FXML;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.geo.GeoBox;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.query.GeoDistanceOrder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author P.J. Meisch (pj.meisch@sothawo.com)
 */
@Component
@FxmlView("/MainController.fxml") // if / is omiited, resource loader expects the fxml in the package
public class MainController {

    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

    private static final int ZOOM_DEFAULT = 14;
    private static final Coordinate coordKarlsruheCastle = new Coordinate(49.013517, 8.404435);

    private final DBStationsConfiguration configuration;
    private final DBStationRepository repository;

    private final List<MapLabel> labelsRight = new ArrayList<>();
    private final Marker markerRight = Marker.createProvided(Marker.Provided.GREEN);

    private final List<MapLabel> labelsLeft = new ArrayList<>();

    private final AtomicBoolean updatingMapLeft = new AtomicBoolean(false);

    @FXML
    private MapView mapViewLeft;

    @FXML
    private MapView mapViewRight;

    public MainController(DBStationsConfiguration configuration, DBStationRepository repository) {
        this.configuration = configuration;
        this.repository = repository;
    }

    @FXML
    public void initialize() {
        initMapViewLeft();
        initMapViewRight();
    }

    private void initMapViewLeft() {
        MapType mapType = MapType.OSM;
        if (configuration.getBingMapsApiKey() != null) {
            mapViewLeft.setBingMapsApiKey(configuration.getBingMapsApiKey());
            mapType = MapType.BINGMAPS_CANVAS_GRAY;
        }
        init(mapViewLeft, mapType);

        mapViewLeft.addEventHandler(MapViewEvent.MAP_BOUNDING_EXTENT, event -> {
            event.consume();

            if (mapViewLeft.getInitialized() && !updatingMapLeft.getAndSet(true)) {
                Extent extent = event.getExtent();
                LOG.debug("extent: {}", extent);

                GeoBox geoBox = new GeoBox(new GeoPoint(extent.getMax().getLatitude(), extent.getMin().getLongitude()),
                    new GeoPoint(extent.getMin().getLatitude(), extent.getMax().getLongitude()));

                SearchHits<DBStation> searchHits = null;
                try {
                    searchHits = repository.searchByLocationNear(geoBox);
                } catch (Throwable t) {
                    LOG.error("OOPS", t);
                }

                displaySearchHitsInMapLeft(searchHits);
                updatingMapLeft.set(false);
            }
        });
    }

    private void initMapViewRight() {
        MapType mapType = MapType.OSM;
        if (configuration.getBingMapsApiKey() != null) {
            mapViewRight.setBingMapsApiKey(configuration.getBingMapsApiKey());
            mapType = MapType.BINGMAPS_ROAD;
        }

        init(mapViewRight, mapType);
        markerRight.setVisible(false);

        mapViewRight.addEventHandler(MapViewEvent.MAP_CLICKED, event -> {
            event.consume();

            if (mapViewRight.getInitialized()) {

                Coordinate newPosition = event.getCoordinate().normalize();
                markerRight.setPosition(newPosition);
                mapViewRight.setCenter(newPosition);
                mapViewLeft.setCenter(newPosition);
                if (!markerRight.getVisible()) {
                    mapViewRight.addMarker(markerRight);
                    markerRight.setVisible(true);
                }

                GeoPoint geoPoint = new GeoPoint(newPosition.getLatitude(), newPosition.getLongitude());

                SearchHits<DBStation> searchHits = repository.searchTop5By(Sort.by(new GeoDistanceOrder("location", geoPoint).withUnit("km")));

                displaySearchHitsInMapRight(searchHits);

                double zoom = mapViewRight.getZoom();
                mapViewLeft.setZoom(zoom);
                mapViewRight.setZoom(zoom);
            }
        });
    }

    private void init(MapView mapView, MapType mapType) {
        mapView.setAnimationDuration(500);
        mapView.setCustomMapviewCssURL(getClass().getResource("/dbstations.css"));
        mapView.initializedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                mapView.setZoom(ZOOM_DEFAULT);
                mapView.setCenter(coordKarlsruheCastle);
            }
        });
        mapView.setMapType(mapType);
        mapView.initialize();
    }

    private void displaySearchHitsInMapLeft(SearchHits<DBStation> searchHits) {
        labelsLeft.forEach(mapViewLeft::removeLabel);
        labelsLeft.clear();

        if (searchHits != null) {
            searchHits.forEach(searchHit -> {
                DBStation dbStation = searchHit.getContent();
                MapLabel mapLabel = new MapLabel("O");
                mapLabel.setPosition(new Coordinate(dbStation.getLocation().getLat(), dbStation.getLocation().getLon()));
                mapLabel.setCssClass(dbStation.getType());
                mapViewLeft.addLabel(mapLabel);
                labelsLeft.add(mapLabel);
            });
            labelsLeft.forEach(mapViewLeft::addLabel);
            labelsLeft.forEach(mapLabel -> mapLabel.setVisible(true));
        }
    }

    private void displaySearchHitsInMapRight(SearchHits<DBStation> searchHits) {
        labelsRight.forEach(mapViewRight::removeLabel);
        labelsRight.clear();
        searchHits.forEach(searchHit -> {
            DBStation dbStation = searchHit.getContent();
            MapLabel mapLabel = new MapLabel(String.format("%1$3.1f km - %2$s", searchHit.getSortValues().get(0), dbStation.getName()));
            mapLabel.setPosition(new Coordinate(dbStation.getLocation().getLat(), dbStation.getLocation().getLon()));
            mapLabel.setCssClass(dbStation.getType());
            mapLabel.setVisible(true);
            mapViewRight.addLabel(mapLabel);
            labelsRight.add(mapLabel);
        });
    }
}
