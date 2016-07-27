jarssApp.directive('wjValidationError', function () {
  return {
    require: 'ngModel',
    link: function (scope, elm, attrs, ctl) {
      scope.$watch(attrs['wjValidationError'], function (errorMsg) {
        elm[0].setCustomValidity(errorMsg);
        ctl.$setValidity('wjValidationError', errorMsg ? false : true);
      });
    }
  };
});

jarssApp.controller('InitialSetupController', function ($scope, $http, $window, $uibModal) {
    $scope.dbtype = "postgresql";
    $scope.dbhost = "localhost";
    $scope.dbname = "jarss";
    
    $scope.alerts = [];
    $scope.closeAlert = function(index) {
      $scope.alerts.splice(index, 1);
    };
    
    $scope.configure = function(valid) {
        if (!valid) {
            console.log("Form not valid");
            return;
        }
            
        var settings = {
            url: "jdbc:" + $scope.dbtype + "://" + $scope.dbhost + "/" + $scope.dbname,
            user: $scope.dbuser,
            password: $scope.dbpassword,
            registrationAllowed: $scope.registrationAllowed
        };
        
        $scope.showSpinner();
        
        $http.post('api/v1/initial-setup/setup', JSON.stringify(settings))
        .then(function() {
    
            $scope.registerUser();
        })
        .catch(function(response) {
            $scope.spinner.dismiss();
    
            if (response.data)
                $scope.alerts.push({ type: 'danger', msg: response.data.error });
            else
                $scope.alerts.push({ type: 'danger', msg: 'Request failed :-(' });
        });
        
        return false;
    };
    
    $scope.registerUser = function() {
        var userData = {
            user: $scope.user,
            password: $scope.password,
            email: $scope.email
        };
        
        $http.post('api/v1/user/register', JSON.stringify(userData))
        .then(function() {
            $window.location.href = "index.html#initial-setup-done";
        })
        .catch(function(response) {
            $scope.spinner.dismiss();
    
            if (response.data)
                $scope.alerts.push({ type: 'danger', msg: response.data.error });
            else
                $scope.alerts.push({ type: 'danger', msg: 'Request failed :-(' });
        }); 
        
    };
    
    $scope.spinner = null;
    $scope.showSpinner = function() {
        $scope.spinner = $uibModal.open({
          animation: true,
          templateUrl: 'applying.html',
        });
    };
});
