
var app = angular.module('handyfinderwebapp', ['ngAnimate','ui.bootstrap', 'apiServiceApp']);


app.controller('mainApplicationController', ['$scope','apiService', function ($scope, apiService) {
	$scope.tempvar = 1;
	
}]);