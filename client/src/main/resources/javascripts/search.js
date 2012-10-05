'use strict'

angular.module('search', [])
  .service('searchService', function($http){
    return {
      search: function(term, callback) {
        $http.get('/search/?term=' + term).success(function(data) {
          var result = [];
          for (var i=0; i<data.length; i++) {
            result.push(data[i]);
          }
          callback && callback.call(this, result);
        });
      }
    };
  })
  .controller('SearchController', ['$scope', '$location', 'searchService',
      function($scope, $location, searchService) {
        $scope.term = "";
        $scope.suggestions = [];
        $scope.clear = function() {
          $scope.term = "";
          $scope.suggestions = [];
        }
        $scope.setRoute = function(handle) {
          $location.url("/product/" + handle);
          $scope.clear();
        }
        $scope.$watch('term', function(term) {
          if (term != "") {
            searchService.search(term, function(result){
              $scope.suggestions = result;
            });
          }
        });
  }]);
