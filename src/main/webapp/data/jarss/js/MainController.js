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

jarssApp.controller('MainController', function ($scope, $http, $uibModal, $sce) {
    $scope.logout = function() {
        $scope.$emit("logout");
    };
	
	$scope.loadFeedsTree = function() {
		$http.get("api/v1/feeds/tree", $scope.httpConfig)
		.then(function (response) {
			$scope.data = response.data;
		})
		.catch(function (response) {
			console.error("Cannot load feeds tree");
		});
	};
    
    $scope.$watch("authenticated", function() {
        if ($scope.authenticated) {
            // Load feeds etc.
            $scope.httpConfig = {
                headers: {
                    'Authorization': 'Bearer ' + $scope.token
                }
            };
            
            $scope.loadFeedsTree();
            
            window.setTimeout(function() {
                Split(['#feeds-pane', '#right-pane'], { gutterSize: 8, cursor: 'col-resize', sizes: [ 25, 75 ] });
                // Split(['#articles-pane', '#preview-pane'], { direction: 'vertical', gutterSize: 8, cursor: 'row-resize' });
            }, 100);

        }
    });
    
    $scope.subscribe = function() {
        var modal = $uibModal.open({
			animation: true,
			templateUrl: 'data/jarss/tpl/subscribe.html',
			controller: 'SubscribeController',
			resolve: {
				token: function() {
					return $scope.token;
				}
			}
		});
		modal.result.then(function (addedNew) {
			if (addedNew)
				$scope.loadFeedsTree();
		});
    };
    
    
    $scope.data = [];
    $scope.selectedNode = null;
    $scope.selectedNodeDetails = {};
    
    $scope.headlines = [];
    $scope.headlinesFetchTime = null; // Used for "Mark all read" functionality
    
    // Feed or feed category selection
    $scope.selectNode = function(node) {
        $scope.selectedNode = node;
        $scope.selectedHeadlineId = -1;
        
        if (node !== null) {
            var where = node.isCategory ? 'categories' : 'feeds';
            
            $scope.selectedNodeDetails = {};
            $scope.headlines = { loading: true };
            
            $http.get("api/v1/" + where + "/" + node.id + "/headlines?limit=50", $scope.httpConfig)
            .then(function(response) {
                $scope.headlines = response.data;
            })
            .catch(function(response) {
                console.log("Error loading feed headlines: " + response);
            });
            
            if (!node.isCategory) {
                $http.get("api/v1/feeds/" + node.id + "/details", $scope.httpConfig)
                .then(function(response) {
                    $scope.selectedNodeDetails = response.data;
                })
                .catch(function(response) {
                    console.log("Error loading feed details: " + response);
                });
            } else {
                $scope.selectedNodeDetails = { title: node.title };
            }
        } else {
            $scope.headlines = [];
        }
    };
    
    $scope.showArticle = function(event, article) {
        if (event.which === 2) // middle button
            return;
        
        event.preventDefault();
        
        $scope.selectedHeadlineId = article.id;
        
        if (!article.description) {
            article.article = { loading: true };
            
            $http.get('api/v1/article/' + article.id, $scope.httpConfig)
            .then(function(response) {
        
                article.article = response.data;
                article.article.description = $sce.trustAsHtml(article.article.description);
                
                window.setTimeout(function() {
                    var el = document.getElementById("content-" + article.id);
                    var links = el.getElementsByTagName("a");
                    
                    for (var i = 0; i < links.length; i++) {
                        links[i].addEventListener("click", function(event) {
                            if (event.which === 1) {
                                event.preventDefault();
                                window.open(this.href, "_blank");
                            }
                        });
                    }
                }, 50);
            })
            .catch(function(response) {
                // TODO: handle error
                article.description = $sce.trustAsHtml('Preview failed to load');
            });
            
            if (!article.read) {
                $http.put('api/v1/article/' + article.id, {'read': true}, $scope.httpConfig)
                .then(function() {
                    article.read = true;
                })
                .catch(function() {
                    console.log("Cannot mark article " + article.id + " as read");
                });
            }
        }
    };
    
    $scope.formatDate = function(millis) {
        var date = new Date(millis);
        var now = new Date();
        
        if (date.toDateString() === now.toDateString()) {
            // show time
            return moment(date).format('LT');
        } else {
            // show date
            return moment(date).format('lll');
        }
    }
    
    $scope.toggleStar = function(event, article) {
        var oldstate = article.starred;
        
        article.starred = !article.starred;
        
        $http.put('api/v1/article/' + article.id, {'starred': article.starred}, $scope.httpConfig)
        .then(function() {
            
        })
        .catch(function() {
            console.log("Cannot mark article " + article.id + " as starred");
            article.starred = oldstate;
        });
        
        event.stopPropagation();
    };
    
    $scope.applyIsRead = function(article) {
        var oldstate = !article.read;
        
        $http.put('api/v1/article/' + article.id, {'read': article.read}, $scope.httpConfig)
        .then(function() {
            
        })
        .catch(function() {
            console.log("Cannot mark article " + article.id + " as (not) read");
            article.read = oldstate;
        });
    };
    
    $scope.openArticle = function(event, link) {
        if (event.which === 2) // middle button
            return;
        
        event.preventDefault();
        window.open(link, '_blank');
    }
    
    $scope.treeOptions = {
        accept: function(sourceNodeScope, destNodesScope, destIndex) {
            return true;
        },
        dropped: function(event) {
            
        },
    };
    
    $scope.markAllAsRead = function() {
        
    };
});

    
 