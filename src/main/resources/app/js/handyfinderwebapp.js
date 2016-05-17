var app = angular.module('handyfinderwebapp', ['ngRoute', 'ngAnimate', 'ui.bootstrap', 'apiServiceApp', 'ngContextMenu', 'indexModelApp', 'websocketModelApp']);

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

app.controller('indexController', ['$q','$log', '$timeout', '$location', '$scope', 'apiService', 'Path', 'ProgressService',
function($q, $log, $timeout, $location, $scope, apiService, Path, progressService) {
	$scope.pathList = [];
	var promise = apiService.getDirectories();
	promise.then(function(msg) {
		$scope.pathList = msg;
		$scope.index_progress_status.addAlertQ(2);
		$scope.startWatch();
	}, function(msg) {
		$scope.index_progress_status.addAlertQ(3);
		$scope.startWatch();
	}, function(msg) {
		$scope.index_progress_status.addAlertQ(3);
		$scope.startWatch();
	});

	$scope.index_manager_status = {
		open : true
	};
	$scope.index_progress_status = {
		open : false,
		progress : false,
		progressItemCount : 0,
		alerts : [{//0
			open : false,
			type : 'success',
			msg : 'Directories are stored on disk!'
		}, {//1
			open : false,
			type : 'danger',
			msg : 'Storing directories is failed on disk!'
		}, {//2
			open : false,
			type : 'success',
			msg : 'Directories are loaded on disk!'
		}, {//3
			open : false,
			type : 'danger',
			msg : 'Loading directories is failed from disk!'
		}],
		alertQ : [],
		addAlertQ : function(index) {
			if ($scope.index_progress_status.alertQ.indexOf($scope.index_progress_status.alerts[index]) != -1)
				return;
			$scope.index_progress_status.alertQ.push($scope.index_progress_status.alerts[index]);
			$scope.index_progress_status.progressItemCount++;
			$scope.index_progress_status.refreshState();
		},
		removeAlertQ : function(index) {
			$scope.index_progress_status.alertQ.splice(index, 1);
			$scope.index_progress_status.progressItemCount--;
			$scope.index_progress_status.refreshState();
		},
		refreshState : function() {
			if ($scope.index_progress_status.progressItemCount > 0 || $scope.progressBarVisible == true) {
				$scope.index_progress_status.progress = true;
				$scope.index_progress_status.open = true;
			} else {
				$scope.index_progress_status.progress = false;
				$scope.index_progress_status.open = false;
			}
		}
	};

	$scope.index_option_status = {
		open : true
	};

	$scope.save = function() {
		var deferred = $q.defer();
		var promise = apiService.updateDirectories($scope.pathList);
		promise.then(function() {
			//$scope.index_progress_status.addAlertQ(0);
			deferred.resolve();
		}, function() {
			$scope.index_progress_status.addAlertQ(1);
			deferred.reject();
		}, function() {
			$scope.index_progress_status.addAlertQ(1);
			deferred.reject();
		});
		return deferred.promise;
	};

	$scope.startWatch = function() {
		$scope.$watchCollection('pathList', function(newNames, oldNames) {
			$scope.save();
			$log.log('update a list of indexes in server');
		});
	};

	$scope.selectDirectory = function() {
		var path = guiService.openDialogAndSelectDirectory();
		alert(path);
		return path;
	};

	$scope.addDirectory = function() {
		var returnedPath = $scope.selectDirectory();
		if (returnedPath != '') {
			var path = Path.createInstance(returnedPath);
			$scope.pathList.push(path);
			alert('pushed path');
		}
	};

	$scope.enableToggle = function(path) {
		path.used = !path.used;
	};

	$scope.recursivelyToggle = function(path) {
		path.recursively = !path.recursively;
	};

	$scope.edit = function() {
		return $scope.selectDirectory();
	};

	$scope.remove = function(path) {
		$timeout(function() {
			var index = $scope.pathList.indexOf(path);
			if (index > -1) {
				$scope.pathList.splice(index, 1);
			}
		}, 100);
	};

	$scope.run = function() {
		var promise = $scope.save();
		promise.then(function(){progressService.sendStartIndex();},function(){},function(){});
	};

	$scope.processIndex = 0;
	$scope.totalProcessCount = 100;
	$scope.processPath = '';
	$scope.state = 'TERMINATE';
	$scope.progressBarVisible = false;

	var promise = progressService.connect();
	promise.then(function(frame) {
		$log.log(frame);
		var progressPromise = progressService.subProgress();
		progressPromise.then(function() {
		}, function(msg) {
			$log.log(msg);
		}, function(progressObject) {
			if (progressObject.state == 'START')
				$scope.progressBarVisible = true;
			else if (progressObject.state == 'TERMINATE')
				$scope.progressBarVisible = false;

			$scope.processIndex = progressObject.processIndex;
			$scope.processPath = progressObject.processPath;
			$scope.totalProcessCount = progressObject.totalProcessCount;
			$scope.state = progressObject.state;
			$scope.index_progress_status.refreshState();
			$log.log(progressObject.processIndex + ", " + progressObject.totalProcessCount + ", " + progressObject.processPath + ", " + progressObject.state);
		});
	}, function() {
	}, function() {
	});

}]);

app.controller('settingController', ['$location', '$scope', 'apiService',
function($location, $scope, apiService) {
	$scope.extension_status = {
		open : true
	};

}]);
