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


jarssApp.controller('RootController', function ($scope, $http, $window, $cookies) {
    $http.get('api/v1/initial-setup/completed').success(function(data) {
        if (!data.configured)
            $window.location.href = "initial-setup.html";
    });
    
    $scope.token = $cookies.get("token");
    $scope.authenticated = $scope.token !== undefined;
    
    $scope.$on("tokenChanged", function(x, token) {
        $scope.token = token;
        $scope.authenticated = true;
        // $cookies.put("token", token);
    });
    $scope.$on("alert", function(x, data) {
        $scope.alerts.push(data);
    });
    
    $scope.$on("logout", function() {
        $scope.token = null;
        $scope.authenticated = false;
    });
    
    $scope.alerts = [];
    $scope.closeAlert = function(index) {
        $scope.alerts.splice(index, 1);
    };
});
