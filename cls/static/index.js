if(typeof console == 'undefined') {
	var console = {log: function() {}};
}

$(document).ready(function() {
	$('#username').focus();

	var showError = function(msg) {
		if(msg) {
			$('#loginerror').text(msg);
			$('#loginerror').css('display', '');
		} else {
			$('#loginerror').css('display', 'none');
		}
	};
	var showMsg = function(msg) {
		if(msg) {
			$('#loginmsg').text(msg);
			$('#loginmsg').css('display', '');
		} else {
			$('#loginmsg').css('display', 'none');
		}
	};

	$('#login_form').submit(function(e) {
		e.preventDefault();
		var params = {
			'username': $('#username').val(),
			'password': $('#password').val(),
		};
		$('.step1_btn').prop('disabled', true);
		$.post('/login', params, function(data) {
			$('#username').val('');
			$('#password').val('');
			if(data.error) {
				showError(data.error);
				$('#username').focus();
			} else {
				$('#step1').css('display', 'none');
				$('#step2').css('display', '');
				onLogin(data.session_key);
			}
		}, 'json').always(function() {
			$('.step1_btn').prop('disabled', false);
		});
	});

	$('#signup_btn').click(function(e) {
		e.preventDefault();
		var params = {
			'username': $('#username').val(),
			'password': $('#password').val(),
		};
		$('.step1_btn').prop('disabled', true);
		$.post('/signup', params, function(data) {
			$('#username').val('');
			$('#password').val('');
			if(data.error) {
				showError(data.error);
			} else {
				showMsg('Your account has been created. You can now login below.');
			}
			$('#username').focus();
		}, 'json').always(function() {
			$('.step1_btn').prop('disabled', false);
		});
	});

	$('#username').keypress(function() {
		if($('#username').val().length > 0) {
			showError();
			showMsg();
		}
	});
});

var onLogin = function(sessionKey) {
	$.post('/version', function(data) {
		if(data.error) {
			$('#versiondiv').text('Error: ' + data.error + '.');
			$('#versiondiv').css('display', '');
		} else if(!data.up_to_date) {
			$('#versiondiv').text('A new version is available! Download it at https://entgaming.net/wc3connect');
			$('#versiondiv').css('display', '');
		}
	}, 'json');

	$.post('/motd', function(data) {
		if(data.motd) {
			$('#motddiv').text(data.motd);
			$('#motddiv').css('display', '');
		}
	}, 'json');

	$.post('/get-config', function(data) {
		if(data.no_broadcast) {
			$('#cfgNoBroadcast').prop('checked', true);
		}
		if(data.enable_lan) {
			$('#cfgEnableLAN').prop('checked', true);
		}
		if(data.disable_in_game_refresh) {
			$('#cfgDisableInGameRefresh').prop('checked', true);
		}
	}, 'json');

	var showWC3Alert = function(id) {
		$('.wc3alert').css('display', 'none');
		$('#' + id).css('display', '');
	};

	$.post('/wc3', function(data) {
		if(data.installation && data.installation.correct_patch && data.installation.backup) {
			$('#wc3good_dir').text(data.installation.path);
			$('#wc3good_btn').data('path', data.installation.path)
			showWC3Alert('wc3gooddiv');
		} else if(data.home_dir) {
			if(data.installation && data.installation.correct_patch) {
				$('#wc3backup_src').text(data.installation.path);
				$('#wc3backup_home').text(data.home_dir);
				$('#wc3backup_btn').data('path', data.installation.path);
				showWC3Alert('wc3backupdiv');
			} else if(data.installation) {
				$('#wc3bad1_dir').text(data.installation.path);
				$('#wc3bad1_home').text(data.home_dir);
				showWC3Alert('wc3bad1div');
			} else {
				$('#wc3bad2_home').text(data.home_dir);
				showWC3Alert('wc3bad2div');
			}
		}
	}, 'json');

	var setGames = function(tbody, games) {
		var existingRows = {};
		tbody.children('tr.game').each(function() {
			var uiRow = $(this);
			var key = uiRow.data('key');
			existingRows[key] = uiRow;
		});
		var lastRow = null;
		for(var gameIdx = 0; gameIdx < games.length; gameIdx++) {
			var game = games[gameIdx];

			if(game.app_game) {
				var cols = [
					game.app_game.location,
					game.gamename,
					game.app_game.map,
					game.app_game.host,
					game.app_game.slots_taken + '/' + game.app_game.slots_total,
				];
			} else {
				var cols = [
					game.location,
					game.gamename,
					game.map,
					'',
					'',
				];
			}
			var key = game.ip + '.' + game.gamename;
			var existingRow = existingRows[key];
			if(existingRow) {
				existingRow.data('uid', game.uid);
				for(var i = 0; i < cols.length; i++) {
					var el = cols[i];
					if(existingRow.children().eq(i).text() != el) {
						existingRow.children().eq(i).text(el);
					}
				}
				if(game.app_game) {
					existingRow.data('gameid', game.app_game.id);
				}
				lastRow = existingRow;
				delete existingRows[key];
			} else {
				var uiRow = $('<tr>')
					.addClass('game')
					.data('key', key)
					.data('uid', game.uid);
				for(var i = 0; i < cols.length; i++) {
					var uiCol = $('<td>').text(cols[i]);
					uiRow = uiRow.append(uiCol);
				}
				if(game.app_game) {
					uiRow = uiRow.data('gameid', game.app_game.id);
				}
				if(lastRow) {
					lastRow.after(uiRow);
					lastRow = uiRow;
				} else {
					tbody.prepend(uiRow);
					lastRow = uiRow;
				}
			}
		}
		for(var key in existingRows) {
			existingRows[key].remove();
		}
	};

	var getRefreshTime = function(counter) {
		if(counter && counter < 3) {
			return 1000;
		} else {
			return 3000;
		}
	};

	var getGames = function(counter) {
		$.post('/games', function(data) {
			setGames($('#public-tbody'), data.publicGames);
			setGames($('#autohost-tbody'), data.autohostGames);
			setGames($('#others-tbody'), data.otherGames);
			setGames($('#unmoderated-tbody'), data.unmoderatedGames);
		}, 'json')
			.always(function() {
				setTimeout(function() {
					getGames(counter + 1);
				}, getRefreshTime(counter));
			});
	};
	$('tbody').on('click', 'tr.game', function() {
		$('tr.game.selected').removeClass('selected');
		$('tr.gameinfo').remove();
		$(this).addClass('selected');
		var uid = $(this).data('uid');
		$.post('/show', {'uid': uid});

		if($(this).data('gameid')) {
			var row = $('<tr>')
				.addClass('gameinfo')
				.data('gameid', $(this).data('gameid'));
			var col = $('<td>')
				.addClass('gameinfo')
				.attr('colspan', 5);
			row = row.append(col);
			$(this).after(row);
			refreshInfo(true);
		}
	});
	$('tbody').on('click', 'tr.gameinfo a', function(e) {
		e.preventDefault();
	});
	var refreshInfo = function(norefresh) {
		var row = $('tr.gameinfo');
		if(row.length == 0) {
			setTimeout(function() {
				refreshInfo();
			}, getRefreshTime());
			return;
		}
		var gameid = row.data('gameid');
		var post = $.post('/gameinfo', {'gameid': gameid}, function(data) {
			row.children().html(data);
		});
		if(!norefresh) {
			post.always(function() {
				setTimeout(function() {
					refreshInfo();
				}, getRefreshTime());
			});
		}
	};
	getGames(1);
	setTimeout(function() {
		refreshInfo();
	}, 3000);

	var setConfig = function() {
		var params = {
			'no_broadcast': $('#cfgNoBroadcast').prop('checked') ? 'yes' : '',
			'enable_lan': $('#cfgEnableLAN').prop('checked') ? 'yes' : '',
			'disable_in_game_refresh': $('#cfgDisableInGameRefresh').prop('checked') ? 'yes' : '',
		};
		$.post('/set-config', params);
	};

	$('#cfgNoBroadcast').change(function() {
		setConfig();
	});

	$('#cfgEnableLAN').change(function() {
		setConfig();
	});

	$('#cfgDisableInGameRefresh').change(function() {
		setConfig();
	});

	var showSettingsInfo = function(msg) {
		$('#settingsinfo').text(msg);
		$('#settingsinfo').css('display', '');
		setTimeout(function() {
			$('#settingsinfo').css('display', 'none');
		}, 5000);
	};

	$('#validate_btn').click(function(e) {
		e.preventDefault();
		$('#settingsinfo').css('display', 'none');
		var params = {
			key: $('#validate_key').val(),
		};
		$('#validate_key').val('');
		$.post('/validate', params, function(data) {
			if(data.error) {
				showSettingsInfo(data.error);
			} else {
				showSettingsInfo('The validation request was sent successfully! If the key is correct, your WC3Connect account should now be validated in your entgaming.net account.');
			}
		}, 'json');
		$('#validateModal').modal('hide')
	});

	$('#wc3backup_btn').click(function(e) {
		e.preventDefault();
		$('#progresserror').css('display', 'none');
		$('#progressprogress').css('display', '');
		$('#progressModal').modal('show');
		var path = $('#wc3backup_btn').data('path');
		$.post('/wc3-backup', {'path': path}, function(data) {
			if(data.error) {
				$('#progresserror').text(data.error);
				$('#progresserror').css('display', '');
				$('#progressprogress').css('display', 'none');
				return;
			}
			$('#progressModal').modal('hide');
			$('#wc3good_dir').text(data.path);
			$('#wc3good_btn').data('path', data.path);
			showWC3Alert('wc3gooddiv');
		}, 'json');
	});

	$('#wc3good_btn').click(function(e) {
		e.preventDefault();
		var path = $('#wc3good_btn').data('path');
		$.post('/wc3-start', {'path': path});
	});
};
