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

jarssApp.controller('SubscribeController', function ($scope, $http, $uibModalInstance, token) {
    $scope.token = token;
    $scope.category = '0';
    $scope.url = '';
    $scope.categoryName = '';

    httpConfig = {
        headers: {
            'Authorization': 'Bearer ' + $scope.token
        }
    };

    $http.get('api/v1/categories', httpConfig)
    .then(function (response) {
        $scope.categories = response.data;
    });

    $scope.subscribe = function() {
        var data = {
                categoryId: $scope.category,
                url: $scope.url
        };

        $http.post('api/v1/feeds', data, httpConfig)
        .then(function(response) {
                $uibModalInstance.close(true);
        })
        .catch(function(response) {
                if (response.data && response.data.error)
                        window.alert(response.data.error);
                else
                        window.alert("Failed to subscribe, request failed");
        });
    }

    $scope.ok = function () {
        if ($scope.category != -1) {
            $scope.subscribe();
        } else {
            // Create a new category first
            var data = { 'name': $scope.categoryName };
            
            $http.post("api/v1/categories", data, httpConfig)
            .then(function(response) {
                var newIdLoc = response.headers('Location');
                var pos = newIdLoc.lastIndexOf('/');
                
                $scope.category = parseInt(newIdLoc.substring(pos+1));
                $scope.categories.push({ id: $scope.category, name: $scope.categoryName });
                $scope.subscribe();
            })
            .catch(function(response) {
                if (response.data.error)
                        window.alert(response.data.error);
                else
                        window.alert("Failed to create a new category, request failed");
            });
        }
    };

    $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
    };
});
