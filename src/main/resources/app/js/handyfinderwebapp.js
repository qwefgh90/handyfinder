var app = angular.module('handyfinderwebapp', ['ngRoute', 'ngAnimate', 'ui.bootstrap', 'apiServiceApp']);

app.config(function($routeProvider) {
	//Module의 config API를 사용하면 서비스 제공자provider에 접근할 수 있다. 여기선 $route 서비스 제공자를 인자로 받아온다.
	$routeProvider
	//$routeProvider의 when 메소드를 이용하면 특정 URL에 해당하는 라우트를 설정한다. 이때 라우트 설정객체를 전달하는데 <ng-view>태그에 삽입할 탬플릿에 해당하는 url을 설정객체의 templateUrl 속성으로 정의한다.
	.when('/search', {
		templateUrl : 'views/search.html',
		controller : 'searchController'
	}).when('/index', {
		templateUrl : 'views/index.html',
		controller : 'indexController'
	}).when('/setting', {
		templateUrl : 'views/setting.html',
		controller : 'settingController'
	})
	//라우트 설정객체에 controller 속성을 통하여 해당 화면에 연결되는 컨트롤러 이름을 설정할 수 있다.
	.otherwise({
		redirectTo : '/search'
	});
	//otherwise 메소드를 통하여 브라우저의 URL이 $routeProivder에서 정의되지 않은 URL일 경우에 해당하는 설정을 할 수 있다. 여기선 ‘/home’으로 이동시키고 있다.
});
app.controller('mainApplicationController', ['$location', '$scope', 'apiService',
function($location, $scope, apiService) {
	$scope.path = '';
	$scope.go = function(path) {
		$location.path(path);
	};
	$scope.$on('$routeChangeStart', function(next, current) {
		$scope.path = $location.path().substring(1);
	});
}]);

app.controller('searchController', ['$location', '$scope', 'apiService',
function($location, $scope, apiService) {
	$scope.tempvar2 = 1;
}]);
app.controller('indexController', ['$location', '$scope', 'apiService',
function($location, $scope, apiService) {
	$scope.oneAtATime = false;

	$scope.groups = [{
		title : 'Dynamic Group Header - 1',
		content : 'Dynamic Group Body - 1'
	}, {
		title : 'Dynamic Group Header - 2',
		content : 'Dynamic Group Body - 2'
	}];

	$scope.items = ['Item 1', 'Item 2', 'Item 3'];

	$scope.addItem = function() {
		var newItemNo = $scope.items.length + 1;
		$scope.items.push('Item ' + newItemNo);
	};
	
	$scope.status = {
		isFirstOpen : true,
		isFirstDisabled : false,
		open: true
	};
	$scope.status2 = {
		isFirstOpen : true,
		isFirstDisabled : false,
		open: true
	};
}]);
app.controller('settingController', ['$location', '$scope', 'apiService',
function($location, $scope, apiService) {
	$scope.tempvar = 1;
}]);

