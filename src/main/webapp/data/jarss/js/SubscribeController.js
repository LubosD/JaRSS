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
	
	httpConfig = {
		headers: {
			'Authorization': 'Bearer ' + $scope.token
		}
	};
	
	$http.get('api/v1/categories', httpConfig)
	.then(function(response) {
		$scope.categories = response.data;
	});
	
    $scope.ok = function () {
		var data = {
			categoryId: $scope.category,
			url: $scope.url
		};
		
		$http.post('api/v1/feeds', data, httpConfig)
		.then(function(response) {
			$uibModalInstance.close(true);
		})
		.catch(function(response) {
			if (response.data.error)
				alert(response.data.error);
			else
				alert("Failed to subscribe, request failed");
		});
        
    };

    $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
    };
});
