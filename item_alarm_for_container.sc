//容器进脏警报器
//  作者：Cupperum29，ai辅助
//  功能概述：三个模式，一个黑名单一个白名单一个数量检测，需先指定容器
//           黑名单模式：容器进入指定的物品即报警  
//                      /<app> blacklist <pos> <item>
//           白名单模式：容器进入非指定的物品即报警  
//                      /<app> whitelist <pos> <item>
//           数量检测模式：再细分四个模式，一个大于等于指定数量则报警，一个小于等于指定数量则报警，一个非指定数量则报警，一个等于指定数量则报警
//                      /<app> capacity <pos> <item> <mode> <capacity>
//           周期：修改与获取当前检测周期
//                 修改周期：
//                      /<app> cycle <tick>
//                 获取当前周期：
//                      /<app> cycle
//


__config() ->
  {
    'commands' ->{
                  //检测周期
                    //修改检测周期
                    'cycle <tick>' -> 'modify_cycle',
                    //获取当前检测周期
                    'cycle' -> 'get_cycle',
                  //黑名单模式
                    'blacklist <pos> <item>' -> 'blacklist',
                  //白名单模式
                    'whitelist <pos> <item>' -> 'whitelist',
                  //数量检测模式
                    //等于（equal是关键字，表示equal，不能作为自定义函数的名字）
                    'capacity <pos> <item> is <capacity>' -> 'equal_mode', 
                    //不等于
                    'capacity <pos> <item> not <capacity>' -> 'not_equal', 
                    //大于等于
                    'capacity <pos> <item> greater <capacity>' -> 'greater', 
                    //小于等于
                    'capacity <pos> <item> less <capacity>' -> 'less'
                    //无需写<=和>=的指令，因为在整数中，>=1和>0等效，<=100和<101等效
                  },
    'arguments' ->
          {
            'tick'-> {
                      'type' -> 'int',//限制输入整数
                      'min' -> 1,//限制为正整数，周期为负数无意义
                      'suggest' -> [1,2,4,8,16,24]
                     },
            'pos' -> {'type' -> 'pos'},
            'item' -> {
                       'type' -> 'item',//限制输入物品id
                       'suggest' -> //建议项：漏斗矿车、矿车、剪刀
                                   [
                                    'hopper_minecart',
                                    'minecart',
                                    'shears'
                                   ]
                      },
            'capacity' -> {
                           'type' -> 'int',//数量检测模式，输入数量，必须为整数
                           'min' -> 0,//限制为正整数，负数无意义（用0不如去用黑名单模式）
                           'max' -> 3457,//最大数量为3457，最大的容器为大箱子，容量为54*64=3456，要考虑开闭，因此要+1，为3457
                           'suggest' -> [23,45,576,1728,3456]//建议项：23——1 6 6 5 5（大吸力分类填法）
                                                             //       45——41 1 1 1 1（小吸力分类填法）
                                                             //       576——一投掷器、发射器的容量
                                                             //       1728——一盒的容量
                                                             //       3456——一个大箱子的容量
                                                             //
                          },
          },
    'scope' -> 'player',//让每个玩家的应用状态相互隔离
    'command_permission' -> _(p) -> (p != null && query(p,'gamemode') == 'creative'),//限定创造模式
  };

//默认变量
cycle = 1;//默认的检测周期，所有模式的检测的周期共用一个周期


//自定义函数

  //统计容器里某种物品的总数
  count_item(pos, item) ->
  (
    total = 0;  slot = 0;
    while
    (
      slot < inventory_size(pos), //遍历整个容器的所有槽位
      inventory_size(pos),//限制循环次数防止死循环
        (
            stack = inventory_get(pos, slot);
            if(stack != null && stack:0 == item, total += stack:1);
            slot += 1;
        )
    );
    return(total);
  );

  //每tick检测容器是否有黑名单物品并打印
  is_blacklist(pos, item) ->
  (
    slot = inventory_find(pos, item);
    if 
    (
      slot != null,
      (
        print(player(),format('w 在第'+tick_time()+'游戏刻,位于'+ pos +'的容器中有'+ item_display_name(inventory_get(pos, slot):0) + '。' ));
        print(player(),format('w 容器进脏警报器（黑名单）已停止。'));
        return();
      )
    );
    schedule(cycle, 'is_blacklist', pos, item);
  );

  //每tick检测容器是否有非白名单物品并打印
  is_whitelist(pos,item) ->
  (
    slot = 0 ;
    while
    (
      slot < inventory_size(pos),//遍历整个容器的所有槽位
      inventory_size(pos), //限制循环次数防止死循环
      if
      (
        inventory_get(pos, slot) != null && inventory_get(pos, slot) != item,  //判断是否为非空非白名单物品
        (
          print(player(),format('w 在第'+tick_time()+'游戏刻,位于'+ pos +'的容器中有非'+ item_display_name(inventory_get(pos, slot):0) + '的物品。'));
          print(player(),format('w 容器进脏警报器（白名单）已停止。'));
          return();
        )
      );
      slot += 1;
    );
    schedule(cycle, 'is_whitelist', pos, item);
  );

  //每tick检测容器的某个物品总数是否等于某个数
  is_equal(pos,item,capacity) ->
  (
    total = count_item(pos, item);
    if
    (
      total == capacity,
      (
        print(player(),format('w 在第'+tick_time()+'游戏刻,位于'+ pos +'的容器中的'+ item_display_name(item) +'数量达到'+ capacity + '。'));
        print(player(),format('w 容器进脏警报器（等于）已停止。'));
        return();
      )
    );
    schedule(cycle, 'is_equal', pos, item, capacity);
  );

  //每tick检测容器的某个物品总数是否不等于某个数
  is_not_equal(pos,item,capacity) ->
  (
    total = count_item(pos, item);
    if
    (
      total != capacity,
      (
        print(player(),format('w 在第'+tick_time()+'游戏刻,位于'+ pos +'的容器中的'+ item_display_name(item) +'数量不再是'+ capacity + '。'));
        print(player(),format('w 容器进脏警报器（不等于）已停止。'));
        return();
      )
    );
    schedule(cycle, 'is_not_equal', pos, item, capacity);
  );

  //每tick检测容器的某个物品总数是否大于某个数
  is_greater(pos,item,capacity) ->
  (
    total = count_item(pos, item);
    if
    (
      total > capacity,
      (
        print(player(),format('w 在第'+tick_time()+'游戏刻,位于'+ pos +'的容器中的'+ item_display_name(item) +'数量超过'+ capacity + '。'));
        print(player(),format('w 容器进脏警报器（大于）已停止。'));
        return();
      )
    );
    schedule(cycle, 'is_greater', pos, item, capacity);
  );

  //每tick检测容器的某个物品总数是否小于某个数
  is_less(pos,item,capacity) ->
  (
    total = count_item(pos, item);
    if
    (
      total < capacity,
      (
        print(player(),format('w 在第'+tick_time()+'游戏刻,位于'+ pos +'的容器中的'+ item_display_name(item) +'数量不足'+ capacity + '。'));
        print(player(),format('w 容器进脏警报器（小于）已停止。'));
        return();
      )
    );
    schedule(cycle, 'is_less', pos, item, capacity);
  );


//指令函数

  //修改检测周期
  modify_cycle(tick) ->
  (
    if
    (
      tick == cycle ,
      (
        print(player(),format('#FF0000 当前检测周期已为'+ cycle +'gt，请勿重复修改。' ));
        return();
      )
    );
    cycle = tick;
    print(player(),format('w 检测周期已修改为'+cycle +'gt。' ));
    print(player(),format('w 当前检测周期为'+ cycle + 'gt。' ));
  );

  //获取当前检测周期
  get_cycle() ->(print(player(),format('w 当前检测周期为'+ cycle + 'gt。' )));

  //黑名单模式
  blacklist(pos, item) ->
  (
    //先判断是否为容器
    if
    (
      inventory_size(pos) = null,
      (
        //建议搭配OMMC的解析坐标点来显示容器位置
        print(player(),format('#FF0000 位于'+ pos +'的方块不是容器，请重新输入坐标。'));
        return();//检测到不是容器则终止函数
      )
    );
    //先提示玩家正在检测,表示所选位置为容器并且能够正常检测
    print(player(),format('w 容器进脏警报器（黑名单）已于第'+ tick_time() + '游戏刻启动。'));
    print(player(),format('w 正在检测位于'+ pos +'的容器中是否进入了'+ item_display_name(item) + '。'));
    print(player(),format('w 请等待……'));
    is_blacklist(pos, item);//每tick检测容器是否有黑名单物品并打印
  );

  //白名单模式
  whitelist(pos, item) ->
  (
    if
    (
      inventory_size(pos) = null,
      (
        print(player(),format('#FF0000 位于'+ pos +'的方块不是容器，请重新输入坐标。'));
        return();
      )
    );
    print(player(),format('w 容器进脏警报器（白名单）已于第'+ tick_time() + '游戏刻启动。'));
    print(player(),format('w 正在检测位于'+ pos +'的容器中是否进入了'+ item_display_name(item) + '以外的物品。'));
    print(player(),format('w 请等待……'));
    is_whitelist(pos, item);
  );
  
  //数量检测模式（等于）
  equal_mode(pos,item,capacity) ->
  ( 
    if
    (
      inventory_size(pos) == null,
      (
        print(player(),format('#FF0000 位于' + pos + '的方块不是容器，请重新输入坐标。'));
        return();
      )
    );
    //输入的数字超出容器容量上限则报警并停止报警
    if
    (
      capacity > inventory_size(pos)*stack_limit(item),
      (
        print(player(),format('#FF0000 ' + '输入的数字不得大于最大容量，请重新输入数量！'));
        print(player(),format('w 位置：'+ pos + '。'));
        print(player(),format('w 容器进脏警报器（等于）已停止。'));
        return();
      )
    );
    print(player(),format('w 容器进脏警报器（等于）已于第'+ tick_time() + '游戏刻启动。'));
    print(player(),format('w 正在检测位于'+ pos +'的容器中是否有'+ capacity +'个'+ item_display_name(item) +'。'));
    print(player(),format('w 请等待……'));
    is_equal(pos, item, capacity);
  );

  //数量检测模式（不等于）
  not_equal(pos,item,capacity) ->
  (

    if
    (
      capacity > stack_limit(item)*inventory_size(pos),
      (
        print(player(),format('#FF0000 输入的数字不得大于最大容量，请重新输入数量！'));
        print(player(),format('w 位置：'+ pos + '。'));
        print(player(),format('w 容器进脏警报器（不等于）已停止。'));
        return();
      )
    );
    is_not_equal(pos,item,capacity);
  );

  //数量检测模式（大于）
  greater(pos,item,capacity) ->
  (
    if
    (
      capacity > stack_limit(item)*inventory_size(pos),
      (
        print(player(),format('#FF0000 输入的数字不得大于最大容量，请重新输入数量！'));
        print(player(),format('w 位置：'+ pos + '。'));
        print(player(),format('w 容器进脏警报器（大于）已停止。'));
        return();
      )
    );
    is_greater(pos,item,capacity);
  );
  
  //数量检测模式（小于）
  less(pos,item,capacity) ->
  (
    if
    (
      capacity = 1,
      print(player(),format('#FF0000 想要检测容器内是否有该物品建议用黑名单模式！'))
    );
  )