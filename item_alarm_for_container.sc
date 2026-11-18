//容器进脏警报器
//  作者：Cupperum29，Deepseek V4.1 Flash辅助
//  功能概述：三个模式，一个黑名单一个白名单一个数量检测，需先指定容器
//           黑名单模式：容器进入指定的物品即报警  
//                      /<app> blacklist <pos> <item>
//           白名单模式：容器进入非指定的物品即报警  
//                      /<app> whitelist <pos> <item>
//           数量检测模式：再细分四个模式，一个大于等于指定数量报警，一个小于等于指定数量报警，一个非指定数量报警，一个等于指定数量报警
//                      /<app> capacity <pos> <item> <mode> <capacity>

__config() ->
  {
    'commands' ->{
                  //黑名单模式
                  'blacklist <pos> <item>' -> 'blacklist',
                  //白名单模式
                  'whitelist <pos> <item>' -> 'whitelist',
        //数量检测模式
          //等于指定数量报警
          'capacity <pos> <item> = <capacity>' -> 'equal', 
          //不等于指定数量报警
          'capacity <pos> <item> != <capacity>' -> 'not_qual', 
          //大于等于指定数量报警
          'capacity <pos> <item> > <capacity>' -> 'greater', 
          //小于等于指定数量报警
          'capacity <pos> <item> < <capacity>' -> 'less'
          //无需写<=和>=的指令，因为在整数中，>=1和>0等效，<=100和<101等效
      },
    'arguments' ->
          {
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
                           'suggest' -> [23,45,576,1728,3456]//建议项目：23——1 6 6 5 5（大吸力分类填法）
                                                             //         45——41 1 1 1 1（小吸力分类填法）
                                                             //         576——一投掷器、发射器的容量
                                                             //         1728——一盒的容量
                                                             //         3456——一个大箱子的容量
                          }
          },
         'scope' -> 'player'//让每个玩家的应用状态相互隔离
  }
//全局变量
global_tick = 1;检测周期
//另外的自定义函数
  //每tick检测容器是否有黑名单物品并打印
  is_blacklist(pos, item) ->
  (
    slot = inventory_find(pos, item);
    if 
    (
      slot != null,
      (
        print(player(),format('w 在第'+tick_time()+'游戏刻,位于'+ pos +'的容器中有'+ item_display_name(item)));
        return(tick_time());
      )
    );
    schedule(global_tick, 'is_blacklist', pos, item)           // 按照检测周期进行检测  
  )

//黑名单模式
blacklist(pos, item) ->
(
  //先获取开始检测的tick数，用于后面报警时显示检测时长
  start_tick = tick_time();
  print(player(),format('w 容器进脏警报器（黑名单）已于第'+ start_tick + '游戏刻启动'));
  //先判断是否为容器
  if
   (inventory_size(pos) = null,
     (
       //建议搭配OMMC的解析坐标点来显示容器位置
       print(player(),format('w 位于'+ pos +'的方块不是容器，请重新输入坐标'));
       //检测到不是容器则终止函数
       return();
     )
   );
  //先提示玩家正在检测
  print(player(),format('w 正在检测位于'+ pos +'的容器中是否有'+ item_display_name(item)));
  //每tick检测容器是否有黑名单物品并打印
  alarm_tick = is_blacklist(pos, item)-start_tick;//获取报警时间,单位：刻
  lasting_time= floor((alarm_tick-start_tick)/20);//计算报警时长,单位：秒，保留整数
  // => "xx时xx分xx秒"
  alarm_time = str('%02d时%02d分%02d秒' , floor(lasting_time / 3600) , floor(lasting_time / 60) % 60 , lasting_time % 60);  
  print(player(),format('w 容器进脏警报器（黑名单）记录（游戏内）时长为'+ alarm_time));
);