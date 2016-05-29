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

app.controller('searchController', ['$location','$log', '$scope', 'apiService', 'Document','$sce', 'GUIService', 'SearchModel',
function($location, $log, $scope, apiService, Document, $sce, GUIService, SearchModel) {
	$scope.searchModel = SearchModel.model;
//	$scope.searchKeyword = ''; 
//	$scope.searchResult = [];
	var searchFlag = true;
	$scope.search = function(keyword){
		if(searchFlag == false)
			return;
		var promise = apiService.search(keyword);
		searchFlag = false;
		$scope.searchModel.searchCount = $scope.searchModel.searchCount + 1;
		promise.then(function(json){
			$scope.searchModel.searchResult = [];
			for(var i = 0 ; i < json.length ; i ++){
				var data = json[i];
				var document = new Document(data.createdTime, data.modifiedTime, data.title, data.pathString, $sce.trustAsHtml(data.contents), data.parentPathString)
				$scope.searchModel.searchResult.push(document);
			}
			searchFlag = true;
		},function(){searchFlag = true;$log.log('search failed')}
		,function(){searchFlag = true;});
	};

	var promise = GUIService.connect();
	promise.then(function(frame) {
		$log.log('[handy]'+frame);
		$scope.open = function(path){
			GUIService.openDirectory(path);
		};
		$scope.open_file = function(path){
			GUIService.openFile(path);
		};
	}, function(error) {
		$log.log('[handy]'+error);
	}, function(noti) {
		$log.log('[handy]'+noti);
	});
}]);

app.controller('indexController', ['$q','$log', '$timeout', '$location', '$scope', 'apiService', 'Path', 'ProgressService', 'IndexModel',
function($q, $log, $timeout, $location, $scope, apiService, Path, progressService, IndexModel) {
	$scope.indexModel = IndexModel.model;
	var promise = apiService.getDirectories();
	
	promise.then(function(msg) {
		$scope.indexModel.pathList = msg;
		$scope.indexModel.index_progress_status.addAlertQ(2);
		$scope.startWatch();
	}, function(msg) {
		$scope.indexModel.index_progress_status.addAlertQ(3);
		$scope.startWatch();
	}, function(msg) {
		$scope.indexModel.index_progress_status.addAlertQ(3);
		$scope.startWatch();
	});
	
	$scope.save = function() {
		var deferred = $q.defer();
		var promise = apiService.updateDirectories($scope.indexModel.pathList);
		promise.then(function() {
			//$scope.indexModel.index_progress_status.addAlertQ(0);
			deferred.resolve();
		}, function() {
			$scope.indexModel.index_progress_status.addAlertQ(1);
			deferred.reject();
		}, function() {
			$scope.indexModel.index_progress_status.addAlertQ(1);
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
		if(guiService == undefined){
			$log.log('you can\'t javafx method in browser');
			return '';
		}
		var path = guiService.openDialogAndSelectDirectory();
		$log.log(path);
		return path;
	};

	$scope.addDirectory = function() {
		var returnedPath = $scope.selectDirectory();
		if (returnedPath != '') {
			var path = Path.createInstance(returnedPath);
			$scope.indexModel.pathList.push(path);
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
			var index = $scope.indexModel.pathList.indexOf(path);
			if (index > -1) {
				$scope.indexModel.pathList.splice(index, 1);
			}
		}, 100);
	};

	$scope.run = function() {
		var promise = $scope.save();
		promise.then(function(){progressService.sendStartIndex();},function(){},function(){});
	};
	
	var promise = progressService.connect();
	promise.then(function(frame) {
			$log.log('[handy]'+frame);
			var progressPromise = progressService.subProgress();
		progressPromise.then(function() {
			}, function(msg) {
				$log.log(msg);
			}, function(progressObject) {
				if (progressObject.state == 'START')
					$scope.indexModel.progressBarVisible = true;
				else if (progressObject.state == 'TERMINATE'){
					$scope.indexModel.progressBarVisible = false;
					$scope.indexModel.index_progress_status.addAlertQ(4);
				}
				$scope.indexModel.processIndex = progressObject.processIndex;
				$scope.indexModel.processPath = progressObject.processPath;
				$scope.indexModel.totalProcessCount = progressObject.totalProcessCount;
				$scope.indexModel.state = progressObject.state;
				$scope.indexModel.index_progress_status.refreshState();
				$log.log(progressObject.processIndex + ", " + progressObject.totalProcessCount + ", " + progressObject.processPath + ", " + progressObject.state);
			});
	}, function(error) {
		$log.log('[handy]'+error);
	}, function(noti) {
		$log.log('[handy]'+noti);
	});

}]);

app.controller('settingController', ['$location', '$scope', 'apiService',
function($location, $scope, apiService) {
	$scope.extension_status = {
		open : true
	};

}]);
/*
app.directive('focusMe', function($timeout, $parse) {
	//http://stackoverflow.com/questions/14833326/how-to-set-focus-on-input-field
	return {
		//scope: true,   // optionally create a child scope
		link : function(scope, element, attrs) {
			var model = $parse(attrs.focusMe);
			scope.$watch(model, function(value) {
				console.log('value=', value);
				if (value === true) {
					$timeout(function() {
						element[0].focus();
					});
				}
			});
			// to address @blesh's comment, set attribute value to 'false'
			// on blur event:
			element.bind('blur', function() {
				console.log('blur');
				scope.$apply(model.assign(scope, false));
			});
		}
	};
});*/
app.directive("compileHtml", function($parse, $sce, $compile) {
    return {
        restrict: "A",
        link: function (scope, element, attributes) {
 
            var expression = $sce.parseAsHtml(attributes.compileHtml);
 
            var getResult = function () {
                return expression(scope);
            };
 
            scope.$watch(getResult, function (newValue) {
                var linker = $compile(newValue);
                element.append(linker(scope));
            });
        }
    }
});