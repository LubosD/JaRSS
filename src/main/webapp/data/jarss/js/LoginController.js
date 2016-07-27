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


jarssApp.controller('LoginController', function ($scope, $http, $cookies) {
    $scope.doLogin = function(valid) {
        if (!valid)
            return;
        
        var authData = { login: $scope.login, password: $scope.password };
        $http.post('api/v1/user/login', authData)
        .then(function(response) {
            console.log("Acquired token: " + response.data.token);
            $scope.$emit("tokenChanged", response.data.token);
        })
        .catch(function(response) {
            if (response.data)
                $scope.$emit("alert", { msg: response.data.error, type: 'danger' });
            else
                $scope.$emit("alert", { msg: 'Request failed' });
        });
    };
});
