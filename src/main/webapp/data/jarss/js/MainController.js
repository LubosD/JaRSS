/* 
 * Copyright (C) 2016 Lubos Dolezel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


jarssApp.controller('MainController', function ($scope, $http) {
    $scope.logout = function() {
        $scope.$emit("logout");
    };
    
    $scope.$watch("authenticated", function() {
        if ($scope.authenticated) {
            // Load feeds etc.
            $scope.httpConfig = {
                headers: {
                    'Authorization': 'Bearer ' + $scope.token
                }
            };
            
            $http.get("api/v1/feeds/tree", $scope.httpConfig)
            .then(function (response) {
                $scope.data = response.data;
            })
            .catch(function (response) {
                console.error("Cannot load feeds tree");
            });
        }
    });
    
    $scope.subscribe = function() {
        
    };
    
    
    Split(['#feeds-pane', '#right-pane'], { gutterSize: 8, cursor: 'col-resize', sizes: [ 25, 75 ] });
    Split(['#articles-pane', '#preview-pane'], { direction: 'vertical', gutterSize: 8, cursor: 'row-resize' });
    
    $scope.data = [];
    $scope.selectedNode = null;
    
    $scope.selectNode = function(node) {
        $scope.selectedNode = node;
        
        // TODO: load articles...
    }
});

