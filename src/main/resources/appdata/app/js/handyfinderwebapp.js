define(['angular', 'angularRoute', 'angularSanitize', 'angularAnimate', 'angularBootstrap'
        , 'ngContextmenu', 'indexModel', 'webSocketModel', 'searchModel', 'optionModel'], function(angular){
	var app = angular.module('handyfinderwebapp', ['ngRoute', 'ngSanitize', 'ngAnimate', 'ui.bootstrap', 'ngContextMenu', 'IndexModel', 'WebSocketModel', 'SearchModel', 'OptionModel', 'angular-inview']);
	app.run(['OptionModel', '$log', function(OptionModel, $log){
		var promise = OptionModel.getOptions();
	}]);
	app.config(function($routeProvider) {
		//Module의 config API를 사용하면 서비스 제공자provider에 접근할 수 있다. 여기선 $route 서비스 제공자를 인자로 받아온다.
		$routeProvider
		//$routeProvider의 when 메소드를 이용하면 특정 URL에 해당하는 라우트를 설정한다. 이때 라우트 설정객체를 전달하는데 <ng-view>태그에 삽입할 탬플릿에 해당하는 url을 설정객체의 templateUrl 속성으로 정의한다.
		.when('/search', {
			templateUrl : 'views/search.html',
			controller : 'searchController'
		}).when('/auth', {
			templateUrl : 'views/auth.html',
			controller : 'authController'
		})
		//라우트 설정객체에 controller 속성을 통하여 해당 화면에 연결되는 컨트롤러 이름을 설정할 수 있다.
		.otherwise({
			redirectTo : '/search'
		});
		//otherwise 메소드를 통하여 브라우저의 URL이 $routeProivder에서 정의되지 않은 URL일 경우에 해당하는 설정을 할 수 있다. 여기선 ‘/home’으로 이동시키고 있다.
	});
	app.controller('MainApplicationController', ['$window', '$location', '$http', '$scope','NativeService', '$log', '$timeout', 'OptionModel', 'IndexModel', 
	                                             function($window, $location, $http, $scope, NativeService, $log, $timeout, OptionModel, IndexModel) {
		//$window.alert('secret key : ' + secretKey);
		$scope.indexModel = IndexModel.model;
		$scope.path = '';
		$scope.go = function(path) {
			$location.path(path);
		};
		
		$scope.initJobs = function(){
			var promise = OptionModel.getOptions();
			promise.then( function(){
				if(OptionModel.model.option.firstStart == true)
					$scope.go('/search');

			}, function(){}, function(){});

			(function connectAndStart(){
				var promise = $scope.indexModel.connect();
				promise.then(function(frame) {
					$log.info('Connection to web socket has been successful');
					$log.debug(frame);
					//$scope.indexModel.run();
				}, function(error) {
					$timeout(function() { connectAndStart(); }, 2000); //connect again
					$log.warn('Connection to web socket has been failed');
					$log.warn(error);
				}, function(noti) {
					$log.debug(noti);
				});
			}());

		};
		
		$scope.$on('$routeChangeStart', function(next, current) {
			$scope.path = $location.path().substring(1);
		});

		$scope.initGUIService = function(){
			if(NativeService.isConnected() == false){
				var promise = NativeService.connect();
				promise.then(function(frame) {
					$log.info(frame);
					$scope.openBrowser = function(){
						NativeService.openHome();
					};
					$scope.open = function(link){
						NativeService.openFile(link);
					}

				}, function(error) {
					$timeout(function() {$scope.initGUIService();}, 2000);//connect again
					$log.error(error);
				}, function(noti) {
					$log.info(noti);
				});
			}else{
				$scope.openBrowser = function(){
					NativeService.openHome();
				};
			}
		};

		$scope.requireUpdate = false;
		$scope.programVersion = "";
		$scope.onlineVersion = "";
		$scope.onlineLink = "";
		$http.get('/version').then(function(response){
			if (response.status == 200) {
				var programVersion = response.data.version.trim() == "" ? 0.001 : parseFloat(response.data.version);
				$log.info('programVersion : ' + programVersion);
				$scope.programVersion = programVersion;
				$http.get('/onlineVersion').then(function(response){
					if (response.status == 200) {
						var onlineVersion = parseFloat(response.data.version.trim());
						$scope.onlineVersion = onlineVersion;
						$scope.onlineLink = response.data.link;
						$log.info('online version/link : ' + onlineVersion + ' / ' + $scope.onlineLink);
						if((onlineVersion > programVersion)){
							$scope.requireUpdate = true;
						}
					}else
						$log.debug('failed ' + angular.toJson(response));
				},function(response){
					$log.debug('failed ' + angular.toJson(response));
				});
				
			}else
				$log.debug('failed ' + angular.toJson(response));
			
		},function(response){
				$log.info('failed ' + angular.toJson(response));
		});
		
		$scope.initJobs();
		$scope.initGUIService();
	}]);

	app.constant('SHOWN_RESULT_COUNT', 20);
	app.constant('LIMIT_INDEXED_FILE_LIST', 20);
	app.controller('authController', ['$q', '$log', '$window', '$scope', '$timeout','$sce', 'NativeService', 'SearchModel','OptionModel', 'SupportTypeModel', 'SupportTypeUI', 'ProgressService', 'IndexModel', 'Path', 'LIMIT_INDEXED_FILE_LIST','SHOWN_RESULT_COUNT',
	                                    function($q, $log, $window, $scope, $timeout, $sce, NativeService, SearchModel, OptionModel, SupportTypeModel, SupportTypeUI, ProgressService, IndexModel, Path, LIMIT_INDEXED_FILE_LIST, SHOWN_RESULT_COUNT) {
		
	}]);
	app.controller('searchController', ['$q', '$log', '$window', '$scope', '$timeout','$sce', 'NativeService', 'SearchModel','OptionModel', 'SupportTypeModel', 'SupportTypeUI', 'ProgressService', 'IndexModel', 'Path', 'LIMIT_INDEXED_FILE_LIST','SHOWN_RESULT_COUNT',
	                                    function($q, $log, $window, $scope, $timeout, $sce, NativeService, SearchModel, OptionModel, SupportTypeModel, SupportTypeUI, ProgressService, IndexModel, Path, LIMIT_INDEXED_FILE_LIST, SHOWN_RESULT_COUNT) {
		$scope.searchModel = SearchModel.model;
		$scope.optionModel = OptionModel.model;
		$scope.isCollapsed = true;
		$scope.supportTypeModel = SupportTypeModel.model;
		$scope.supportTypeUI = new SupportTypeUI(SupportTypeModel);
		$scope.LIMIT_INDEXED_FILE_LIST = LIMIT_INDEXED_FILE_LIST;
		$scope.SHOWN_RESULT_COUNT = SHOWN_RESULT_COUNT;
		
		/**
		 * utility
		 */
		$scope.getNumber = function(n) {	
			if(typeof n == "number")
				return new Array(parseInt(n));
			else
				return [];
		}
		
		/**
		 * handler for inview 
		 */
		$scope.elementInViewport = function(object, inview, inviewpart){
			if(inview == true && object.loaded == false){
				var promise = SearchModel.lazyLoadDocumentContent(object);
				promise.then(function(){
					object.loaded = true;
				});
			}
		}
		
		/**
		 * code for directory
		 */
		
		// Select a directory in native file selector
		$scope.selectDirectory = function(originalPath) {
			ProgressService.openDirectoryDialog();
		};

		// Save directories to the server
		$scope.save = function() {
			var deferred = $q.defer();
			var promise = IndexModel.updateDirectories();
			promise.then(function() {
				deferred.resolve();
				$log.debug('successful update a list of path');
			}, function() {
				$scope.indexModel.index_progress_status.addAlertQ(1);
				deferred.reject();
				$log.error('fail to update a list of path');
			}, function() {
				$scope.indexModel.index_progress_status.addAlertQ(1);
				deferred.reject();
			});
			return deferred.promise;
		};
		
		// Add directory to list and save it
		$scope.addDirectory = function(path) {
			var returnedPath = path;
			if (returnedPath != '') {
				var path = Path.createInstance(returnedPath);
				$scope.indexModel.pathList.push(path);
				$scope.save();
				$log.debug('pushed path: '+ returnedPath);
			}
		};

		// Watch changes in directories
		$scope.startWatch = function() {
			$scope.$watchCollection('indexModel.pathList', function(newNames, oldNames) {
				$scope.save();
			});
		};
		
		// Subscribe content notified through websocket
		$scope.registerProgressService = function(){
			var progressPromise = ProgressService.subProgress();
			progressPromise.then(function() {
			}, function(msg) {
				$log.error(msg);
			}, function(progressObject) {
				if (progressObject.state == 'TERMINATE'){
					$scope.indexModel.index_progress_status.progressBarVisible = false;
					$scope.indexModel.index_progress_status.addAlertQ(4);
				}else
					$scope.indexModel.index_progress_status.progressBarVisible = true;
				$scope.indexModel.processIndex = progressObject.processIndex;
				$scope.indexModel.processPath = progressObject.processPath;
				$scope.indexModel.totalProcessCount = progressObject.totalProcessCount;
				$scope.indexModel.state = progressObject.state;
				$scope.indexModel.index_progress_status.refreshState();
				$log.debug(progressObject.processIndex + ", " + progressObject.totalProcessCount + ", " + progressObject.processPath + ", " + progressObject.state);
				IndexModel.getDocumentCount();
			});
			var updatePromise = ProgressService.subUpdate();
			updatePromise.then(function() {
			}, function(msg) {
				$log.error(msg);
			}, function(summaryObject) {
				$scope.indexModel.updateSummary.state = summaryObject.state;
				$scope.indexModel.updateSummary.countOfDeleted = summaryObject.countOfDeleted;
				$scope.indexModel.updateSummary.countOfExcluded = summaryObject.countOfExcluded;
				$scope.indexModel.updateSummary.countOfModified = summaryObject.countOfModified;

				if(summaryObject.state == 'TERMINATE'){
					$scope.indexModel.index_progress_status.setMsgAlertQ(5, 'Update summary -> deleted files : ' + summaryObject.countOfDeleted 
							+ ', non contained files : ' + summaryObject.countOfExcluded + ', modified files : ' + summaryObject.countOfModified);
					$scope.indexModel.index_progress_status.addAlertQ(5);
					$log.debug(summaryObject.countOfDeleted + ", " + summaryObject.countOfExcluded + ", " + summaryObject.countOfModified);
					IndexModel.getDocumentCount();
				}
			});

			var guiDirPromise = ProgressService.subGuiDirectory();
			guiDirPromise.then(function(){},function(msg){$log.error(msg);},
					function(path){
						$log.info('selected path : ' + path);
						$scope.addDirectory(path);
					});
		};
		
		// Connect to a server via a web socket
		(function initProgressService(){
			if(ProgressService.isConnected() == false){
				$timeout(function() { initProgressService(); }, 2000); //connect again
				$log.warn('Socket connection has not yet been ready. Try connecting again...');
			}else{
				if($scope.indexModel.indexControllerSubscribed == false)
					$scope.registerProgressService();
				$scope.indexModel.indexControllerSubscribed = true;
			}
		}());
		
		// Get pathlist from apiserver
		IndexModel.getDirectories().then(function(msg) {
			$log.debug('directories loaded');
			$scope.indexModel.pathList = msg;
			$scope.indexModel.index_progress_status.addAlertQ(2);
			$scope.startWatch();
		}, function(msg) {
			$log.error('directories fail to load');
			$scope.indexModel.index_progress_status.addAlertQ(3);
			$scope.startWatch();
		}, function(msg) {
			$log.debug('directories fail to load');
			$scope.indexModel.index_progress_status.addAlertQ(3);
			$scope.startWatch();
		});
		
		/**
		 * for indexed file list and count
		 */
		
		$scope.loadIndexedFileList = function(){
			var promise = IndexModel.model.indexedFileList.load();
			promise.then(function(){IndexModel.model.indexedFileList.show = true;});
		};
		
		$scope.closeIndexFileList = function(){
			IndexModel.model.indexedFileList.show = false;
		};
		
		$scope.offset = 0;
		$scope.nextList = function(){
			var nextOffset = $scope.offset + LIMIT_INDEXED_FILE_LIST
			if(nextOffset < IndexModel.model.indexedFileList.list.length)
				$scope.offset = nextOffset;
		};
		
		$scope.previousList = function(){
			var previousOffset = $scope.offset - LIMIT_INDEXED_FILE_LIST
			if(0 > previousOffset)
				$scope.offset = 0;
			else
				$scope.offset = previousOffset;
		};

		IndexModel.getDocumentCount();
		
		/**
		 * for context menu
		 */
		
		$scope.enableToggle = function(path) {
			path.used = !path.used;
			$scope.save();
		};

		$scope.recursivelyToggle = function(path) {
			path.recursively = !path.recursively;
		};

		$scope.remove = function(path) {
			$timeout(function() {
				var index = $scope.indexModel.pathList.indexOf(path);
				if (index > -1) {
					$scope.indexModel.pathList.splice(index, 1);
				}
			}, 100);
		};

		/**
		 * Index actions
		 */
		
		// go to page
		$scope.go = function(page){
			if(typeof page == "number"){
				$scope.searchModel.page = parseInt(page);
				$window.scrollTo(0,0);
			}
		};
		
		/**
		 * Code for support types 
		 */
		
		// show first result to contain keyword
		$scope.changeSearchKeyword = function(searchedTypeKeyword){
			$scope.supportTypeUI.changeSearchKeyword(searchedTypeKeyword);
		};
		
		//show next result to contain keyword
		$scope.nextSearch = function(searchedTypeKeyword){
			$scope.supportTypeUI.nextSearch(searchedTypeKeyword);
		}

		//watch changes in support types
		$scope.$watch('supportTypeModel.supportTypes', function(){
			for(var i=0; i<SupportTypeModel.model.supportTypes.length; i++){
				if(SupportTypeModel.model.supportTypes[i].used == false){
					$scope.indexModel.select_toggle = false;
					return;
				}
			}
			$scope.indexModel.select_toggle = true;
		}, true);
		
		//load more data from server
		$scope.loadMore = function(){
			$scope.supportTypeUI.loadMore();
		};
		
		//update data in server
		$scope.updateType = function(obj) {
			var promise = SupportTypeModel.updateSupportType(obj);
			promise.then(function(){
				$log.debug('successful update ' + obj.type + ':' + obj.used);
			},function(){
				$log.error('fail to update ');},function(){});
		};

		//click top element and change all check state 
		$scope.toggleTopcheckbox = function(){
			$log.debug('toggle top checkbox : ' + $scope.indexModel.select_toggle);
			for (var i=0; i<SupportTypeModel.model.supportTypes.length; i++){
				SupportTypeModel.model.supportTypes[i].used = $scope.indexModel.select_toggle;
			}
			$scope.updateSupportTypeList(); //update to api server
		};
		
		//update support data in server
		$scope.updateSupportTypeList = function() {
			SupportTypeModel.updateSupportTypeList();
		};

		
		$scope.search = function(keyword){
			//collapse option panel 
			$scope.isCollapsed = true;
			SearchModel.search(keyword);
		};

		$scope.updateOption = function(){
			OptionModel.updateOptions();
		};

		$scope.initGUIService = function(){
			if(NativeService.isConnected() == false){
				var promise = NativeService.connect();
				promise.then(function(frame) {
					$log.info(frame);
					$scope.open = function(path){
						NativeService.openDirectory(path);
					};
					$scope.openFile = function(path){
						NativeService.openFile(path);
					};
				}, function(error) {
					$timeout(function() {$scope.initGUIService(); }, 2000); //connect again
					$log.error('[handy]'+error);
				}, function(noti) {
					$log.info('[handy]'+noti);
				});
			}else{
				$scope.open = function(path){
					NativeService.openDirectory(path);
				};
				$scope.openFile = function(path){
					NativeService.openFile(path);
				};
			}
		};
		
		$scope.initGUIService();
	}]);

	app.constant('RUNNING_INTERVAL', 10000);
	app.controller('indexController', ['$q','$log', '$timeout', '$location', '$scope', '$interval', 'Path', 'ProgressService', 'IndexModel', 'OptionModel', 'SupportTypeModel', 'SupportTypeUI', 'RUNNING_INTERVAL',
	                                   function($q, $log, $timeout, $location, $scope, $interval, Path, progressService, IndexModel, OptionModel, SupportTypeModel, SupportTypeUI, RUNNING_INTERVAL) {
		$scope.indexModel = IndexModel.model;
		$scope.optionModel = OptionModel.model;
		$scope.supportTypeModel = SupportTypeModel.model;
		$scope.supportTypeUI = new SupportTypeUI(SupportTypeModel);

		var directoriesPromise = IndexModel.getDirectories();	

		$scope.indexModel.select_toggle = false;
		$scope.changeSearchKeyword = function(searchedTypeKeyword){
			$scope.supportTypeUI.changeSearchKeyword(searchedTypeKeyword);
		};

		$scope.nextSearch = function(searchedTypeKeyword){
			$scope.supportTypeUI.nextSearch(searchedTypeKeyword);
		}

		$scope.loadMore = function(){
			$scope.supportTypeUI.loadMore();
		};

		$scope.save = function() {
			var deferred = $q.defer();
			var promise = IndexModel.updateDirectories();
			promise.then(function() {
				//$scope.indexModel.index_progress_status.addAlertQ(0);
				deferred.resolve();
				$log.debug('successful update a list of path');
			}, function() {
				$scope.indexModel.index_progress_status.addAlertQ(1);
				deferred.reject();
				$log.error('fail to update a list of path');
			}, function() {
				$scope.indexModel.index_progress_status.addAlertQ(1);
				deferred.reject();
			});
			return deferred.promise;
		};

		$scope.saveOption = function() {
			OptionModel.updateOptions();
		}

		$scope.startWatch = function() {
			$scope.$watchCollection('indexModel.pathList', function(newNames, oldNames) {
				$scope.save();
			});
		};

		$scope.selectDirectory = function(originalPath) {
			progressService.openDirectoryDialog();
		};

		$scope.addDirectory = function(path) {
			var returnedPath = path;
			if (returnedPath != '') {
				var path = Path.createInstance(returnedPath);
				$scope.indexModel.pathList.push(path);
				$scope.save();
				$log.debug('pushed path: '+ returnedPath);
			}
		};

		{ //context menu handler bind
			$scope.enableToggle = function(path) {
				path.used = !path.used;
			};

			$scope.recursivelyToggle = function(path) {
				path.recursively = !path.recursively;
			};

			$scope.remove = function(path) {
				$timeout(function() {
					var index = $scope.indexModel.pathList.indexOf(path);
					if (index > -1) {
						$scope.indexModel.pathList.splice(index, 1);
					}
				}, 100);
			};
		}

		$scope.run = function() {
			var promise = $scope.save();
			promise.then(function(){
				$scope.indexModel.run();
			},function(){},function(){});
		};

		$scope.stop = function(){
			$scope.indexModel.stop();
		}

		$scope.updateType = function(obj) {
			var promise = SupportTypeModel.updateSupportType(obj);
			promise.then(function(){
				$log.debug('successful update ' + obj.type + ':' + obj.used);
			},function(){
				$log.error('fail to update ');},function(){});
		};

		//click top element of check boxes
		$scope.toggleTopcheckbox = function(){
			$log.debug('toggle top checkbox : ' + $scope.indexModel.select_toggle);
			for (var i=0; i<SupportTypeModel.model.supportTypes.length; i++){
				SupportTypeModel.model.supportTypes[i].used = $scope.indexModel.select_toggle;
			}
			$scope.updateSupportTypeList(); //update to api server
		}
		
		$scope.updateSupportTypeList = function() {
			SupportTypeModel.updateSupportTypeList();
		}

		$scope.registerProgressService = function(){
			var progressPromise = progressService.subProgress();
			progressPromise.then(function() {
			}, function(msg) {
				$log.error(msg);
			}, function(progressObject) {
				if (progressObject.state == 'TERMINATE'){
					$scope.indexModel.index_progress_status.progressBarVisible = false;
					$scope.indexModel.index_progress_status.addAlertQ(4);
				} else
					$scope.indexModel.index_progress_status.progressBarVisible = true;
				$scope.indexModel.processIndex = progressObject.processIndex;
				$scope.indexModel.processPath = progressObject.processPath;
				$scope.indexModel.totalProcessCount = progressObject.totalProcessCount;
				$scope.indexModel.state = progressObject.state;
				$scope.indexModel.index_progress_status.refreshState();
				$log.debug(progressObject.processIndex + ", " + progressObject.totalProcessCount + ", " + progressObject.processPath + ", " + progressObject.state);
				IndexModel.getDocumentCount();
			});
			var updatePromise = progressService.subUpdate();
			updatePromise.then(function() {
			}, function(msg) {
				$log.error(msg);
			}, function(summaryObject) {
				$scope.indexModel.updateSummary.state = summaryObject.state;
				$scope.indexModel.updateSummary.countOfDeleted = summaryObject.countOfDeleted;
				$scope.indexModel.updateSummary.countOfExcluded = summaryObject.countOfExcluded;
				$scope.indexModel.updateSummary.countOfModified = summaryObject.countOfModified;

				if(summaryObject.state == 'TERMINATE'){
					$scope.indexModel.index_progress_status.setMsgAlertQ(5, 'Update summary -> deleted files : ' + summaryObject.countOfDeleted 
							+ ', non contained files : ' + summaryObject.countOfExcluded + ', modified files : ' + summaryObject.countOfModified);
					$scope.indexModel.index_progress_status.addAlertQ(5);
					$log.debug(summaryObject.countOfDeleted + ", " + summaryObject.countOfExcluded + ", " + summaryObject.countOfModified);
					IndexModel.getDocumentCount();
				}
			});

			var guiDirPromise = progressService.subGuiDirectory();
			guiDirPromise.then(function(){},function(msg){$log.error(msg);},
					function(path){
						$log.info('selected path : ' + path);
						$scope.addDirectory(path);
					});
		}
		
		$scope.initProgressService = function(){
			if(progressService.isConnected() == false){
				$timeout(function() { $scope.initProgressService(); }, 2000); //connect again
				$log.error('Socket connection is not ready');
			}else{
				if($scope.indexModel.indexControllerSubscribed == false)
					$scope.registerProgressService();
				$scope.indexModel.indexControllerSubscribed = true;
			}
		}
		
		
		$scope.initProgressService();

		//pathlist from apiserver
		directoriesPromise.then(function(msg) {
			$log.debug('directories loaded');
			$scope.indexModel.pathList = msg;
			$scope.indexModel.index_progress_status.addAlertQ(2);
			$scope.startWatch();
		}, function(msg) {
			$log.error('directories fail to load');
			$scope.indexModel.index_progress_status.addAlertQ(3);
			$scope.startWatch();
		}, function(msg) {
			$log.debug('directories fail to load');
			$scope.indexModel.index_progress_status.addAlertQ(3);
			$scope.startWatch();
		});

		$scope.$watch('supportTypeModel.supportTypes', function(){
			for(var i=0; i<SupportTypeModel.model.supportTypes.length; i++){
				if(SupportTypeModel.model.supportTypes[i].used == false){
					$scope.indexModel.select_toggle = false;
					return;
				}
			}
			$scope.indexModel.select_toggle = true;
		}, true);

		IndexModel.getDocumentCount();
	}]);

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
	
	//https://stackoverflow.com/questions/12790854/angular-directive-to-scroll-to-a-given-item/28369575#28369575 written by Mat
	app.directive('scrollIf', function () {
		var getScrollingParent = function(element) {
			element = element.parentElement;
			while (element) {
				if (element.scrollHeight !== element.clientHeight) {
					return element;
				}
				element = element.parentElement;
			}
			return null;
		};
		return function (scope, element, attrs) {
			scope.$watch(attrs.scrollIf, function(value) {
				if (value) {
					var sp = getScrollingParent(element[0]);
					var topMargin = parseInt(attrs.scrollMarginTop) || 0;
					var bottomMargin = parseInt(attrs.scrollMarginBottom) || 0;
					var elemOffset = element[0].offsetTop;
					var elemHeight = element[0].clientHeight;

					if (elemOffset - topMargin < sp.scrollTop) {
						sp.scrollTop = elemOffset - topMargin;
					} else if (elemOffset + elemHeight + bottomMargin > sp.scrollTop + sp.clientHeight) {
						sp.scrollTop = elemOffset + elemHeight + bottomMargin - sp.clientHeight;
					}
				}
			});
		}
	});
	return app;
});