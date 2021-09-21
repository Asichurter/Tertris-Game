
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Random;

public class GameRun {
	public static void main(String[] args) {
		Thread mythread = new Thread(()->{
			try {
				GameFrame frame = new GameFrame();
			while (true) {
				if (!frame.getIfPause())
					frame.GamePlay();	
				Thread.sleep(250);
				}
			}
			catch (GameException | InterruptedException e) {
				e.printStackTrace();
			}
		});
		mythread.start();
	}
}

/**
 * 用于表征游戏内容错误的异常类
 * @author Asichurter
 *
 */
class GameException extends Exception{

	public GameException(String message) {
		super(message);
	}

}


/**
 * 容纳游戏的主框架
 * @author Asichurter
 *
 */
class GameFrame extends JFrame{
	
	/**
	 * 玩家1的框
	 */
	private GamePanel gamepanel1;
	/**
	 * 玩家2的框
	 */
	private GamePanel gamepanel2;
	/**
	 * 是否暂停
	 */
	private boolean ifPause = false;

	public GameFrame() throws GameException {
		this.setTitle("俄罗斯方块");
		this.setVisible(true);
		this.setBounds(100, 0, 1360, 810);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setResizable(false);
		
		this.gamepanel1 = new GamePanel();
		this.gamepanel2 = new GamePanel();
		JPanel combine = new JPanel();
		combine.setLayout(new GridLayout(1, 2));
		combine.add(gamepanel1);
		combine.add(gamepanel2);
		this.add(combine);
		
		/**
		 * 添加按键监听器
		 */
		this.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				
				//暂停时除了继续键其他键都不可用
				if (GameFrame.this.ifPause && e.getKeyCode() != KeyEvent.VK_ESCAPE)
					return;
				
				switch(e.getKeyCode()) {
				case KeyEvent.VK_A:
					gamepanel1.currentMove(true);
					GameFrame.this.repaint();
					break;
				case KeyEvent.VK_LEFT:
					gamepanel2.currentMove(true);
					GameFrame.this.repaint();
					break;
				case KeyEvent.VK_D:
					gamepanel1.currentMove(false);
					GameFrame.this.repaint();
					break;
				case KeyEvent.VK_RIGHT:
					gamepanel2.currentMove(false);
					GameFrame.this.repaint();
					break;
				case KeyEvent.VK_ESCAPE:
					GameFrame.this.ifPause = !GameFrame.this.ifPause;
					break;
				case KeyEvent.VK_SPACE:
					try {
						gamepanel1.rotateCurrent();
						GameFrame.this.repaint();
					}
					catch (CloneNotSupportedException | GameException e1) {
						e1.printStackTrace();
					}
					break;
				case KeyEvent.VK_ENTER:
					try {
						gamepanel2.rotateCurrent();
						GameFrame.this.repaint();
					}
					catch (CloneNotSupportedException | GameException e1) {
						e1.printStackTrace();
					}
					break;
				default:
					break;
				}
			}
		});
	}
	
	/**
	 * 检查是否游戏结束
	 * @return 是否结束
	 */
	public boolean getIfGameOver() {
		return this.gamepanel1.getIfGameOver() && this.gamepanel2.getIfGameOver();
	}
	
	/**
	 * 检查是否暂停
	 * @return 是否暂停
	 */
	public boolean getIfPause() {
		return this.ifPause;
	}
	
	/**
	 * 游戏进行
	 * @throws GameException 游戏过程中抛出的异常
	 */
	public void GamePlay() throws GameException {
		if(!gamepanel1.getIfGameOver())gamepanel1.GameOn();
		if(!gamepanel2.getIfGameOver())gamepanel2.GameOn();
		this.repaint();
	}
	
	public void  HoldOn() {
		//...
	}
}

/**
 * 用于显示游戏主界面的
 * @author Asichurter
 *
 */
class GamePanel extends JPanel{
	
	/**
	 * 容纳方块的棋盘
	 */
	private boolean[][] GameBoard;
	/**
	 * 当前正在运动的骨牌单位
	 */
	private Tetrimino CurrentOne;
	/**
	 * 下一个骨牌单位
	 */
	private Tetrimino NextOne;
	/**
	 * 
	 */
	private final int Width = 35;
	/**
	 * 是否游戏结束
	 */
	private boolean ifGameOver = false;
	/**
	 * 得分
	 */
	private int Score = 0;

	/**
	 * 初始化界面
	 * @throws GameException 游戏异常
	 */
	public GamePanel() throws GameException {
		this.GameBoard = new boolean[13][23];
		this.CurrentOne = Tetrimino.getRandomOne();
		this.NextOne = Tetrimino.getRandomOne();
	}
	
	/**
	 * 在进行测试性前进后，判断碰撞是否发生
	 * @return 是否发生碰撞
	 * @throws GameException 坐标溢出错误
	 */
	private boolean testIfCollide() throws GameException {
		for (int i = 0; i <= 3; i++) {
			Point p = CurrentOne.getPoint(i);
			if (p.getY() == 22)																											//是否触底
				return true;
			else if (p.getX() < 0 || p.getX() > 12 || p.getY() < 0 || p.getY() > 22)
				continue;
			else if (GameBoard[p.getX()][p.getY()]) {											//发生了碰撞，即骨牌的点处已经有骨牌
				return true;																			
			}
		}
		return false;
	}
	
	/**
	 * 在发生碰撞以后，将当前移动的骨牌固定并且重置移动骨牌。该方法会调用检查消行的方法
	 * @throws GameException 坐标溢出错误
	 */
	private void resetCurrentOne() throws GameException {
		for (int i = 0; i <= 3; i++) {
			Point p = CurrentOne.getPoint(i);
			if (p.getY() < 0) {																																			//如果被固定的骨牌有点处于负位置，则游戏技术
				this.ifGameOver = true;
				return;
			}
			GameBoard[p.getX()][p.getY()] = true;																			//先将骨牌的点固定在棋盘上
		}
		int lines = 0;
		for (int i = 0; i <= 3; i++) {
				if (checkAndRemoveLine(CurrentOne.getPoint(i).getY())) {						//再检查消行
					lines++;
					i = 0;
				}
		}
		switch(lines) {																																		//根据消行数量进行加分
		case 1:
			Score += 10;
			break;
		case 2:
			Score += 40;
			break;
		case 3:
			Score += 90;
			break;
		case 4:
			Score += 160;
			break;
		default:
			break;
		}
		CurrentOne = NextOne;
		NextOne = Tetrimino.getRandomOne();																	//再刷新当前骨牌
	}
	
	/**
	 * 检查一行并且消行
	 * @param index 待检查的行数
	 * @return 该行是否被消去
	 * @throws GameException 坐标溢出的异常
	 */ 
	private boolean checkAndRemoveLine(int index) throws GameException {
		try {
			boolean ifAll = true;
			for (int i = 0; i<= 12; i++) {
				if (!GameBoard[i][index])
					ifAll = false;
			}
			if (ifAll) {																					//如果一行全部都有单位，则消去并且移动
				for (int i = 0; i <= 12; i++) {
					GameBoard[i][index] = false;								//全部消去
				}
				moveLine(index);														//移动前面的行
			}
			return ifAll;
		}
		catch(Exception e) {
			GameException E =  new GameException("在检查并且消行时产生了异常");
			E.initCause(e);
			throw E;
		}
	}
	
	/**
	 * 将index以上的行都向下移动，这是消行的辅助方法
	 * @param index 刚被消去的行
	 */
	private void moveLine(int index) {
		for (int i = index; i >= 1; i--) {
			for (int j = 0; j <= 12; j++) {
				GameBoard[j][i] = GameBoard[j][i-1];
			}
		}
	}
	
	/**
	 * 非按键时游戏继续
	 * @throws GameException
	 */
	public void GameOn() throws GameException {
		CurrentOne.move(true);
		if (testIfCollide()) {													//如果发生了碰撞，就重置状态，同时消行
			CurrentOne.move(false); 								//先后退，将骨牌返回到原来移动前位置
			resetCurrentOne();											//然后再将骨牌固定在棋盘上，同时检查消行并刷新下一个骨牌
		}
		//如果没有发生碰撞，则不讲骨牌退回
	}
	
	/**
	 * 当前骨牌左右移动
	 * @param dir 移动方向
	 */
	public void currentMove(boolean dir) {
		CurrentOne.parallelMove(dir, this.GameBoard);	
	}
	
	/**
	 * 旋转当前骨牌
	 * @throws CloneNotSupportedException 克隆不支持的异常
	 * @throws GameException 坐标溢出的异常
	 */
	public void rotateCurrent() throws CloneNotSupportedException, GameException {
		Tetrimino temp = CurrentOne.clone();										//使用克隆对象进行测试
		temp.rotate();
		for (Point p : temp.Units) {
			if (p.getY() < 0)																													//在负位置不可能出现重复现象，跳过当前回合
				continue;
			else if (GameBoard[p.getX()][p.getY()])												//如果发现旋转以后发生重合，则直接跳出
				return;
		}
		//如果发现没有重合
		CurrentOne.rotate();
	}
	
	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		for (int i = 0; i <= 12; i++) {
			for (int j = 0; j <= 22; j++) {
				if (GameBoard[i][j]) {
					g2.setColor(new Color(100, 180, 40));
					g2.fillRect(i*Width, j*Width, Width, Width);
					g2.setColor(Color.black);
					g2.drawRect(i*Width, j*Width, Width, Width);
				}
			}
		}
		CurrentOne.paint(g2, Width);
		NextOne.tipsPaint(g2, 25);
		g2.setColor(Color.BLACK);
		g2.setFont(g2.getFont().deriveFont(20.0F));
		g2.drawString("得分：" + Score, 0, 20);
		g2.drawLine(0, 0, 0, 810);
		g2.drawLine(455, 0, 455, 810);
		if(ifGameOver) {
			g2.setColor(Color.red);
			g2.setFont(g2.getFont().deriveFont(40.0F));
			g2.drawString("GameOver", 200, 390);
		}
	}
	
	public boolean getIfGameOver() {
		return this.ifGameOver;
	}
}

/**
 * 所有四格骨牌的公有父类
 * @author Asichurter
 *
 */
abstract class Tetrimino implements Cloneable{

	/**
	 * 骨牌的形状
	 */
	protected int Shape;
	/**
	 * 骨牌内的点
	 */
	protected Point[] Units;
	/**
	 * 用于生成随机数的对象
	 */
	private static Random rand = new Random();

	
	/**
	 * 创造一个新的四格骨牌
	 * @param shape 骨牌的形状
	 * @param units 骨牌内的点
	 */
	public Tetrimino(int shape, Point[] units) {
		this.Shape = shape;
		this.Units = units;
	}
	
	/**
	 * 骨牌上下移动，可以通过设置参数进行测试性移动
	 * @param ifAhead 前进还是后退
	 */
	public  final void move(boolean ifAhead) {
		for (Point p: Units) {
			p.setY(p.getY() + (ifAhead ? 1 : -1));
		}
	}
	
	/**
	 * 左右平行移动
	 * @param ifLeft 是否向左，否则向右
	 * @param b 用于检测旋转合法性的已有棋盘
	 */
	public final void parallelMove(boolean ifLeft, boolean[][] b) {
		for (Point p: Units) {																												//先测试移动坐标会不会越界
			if (ifLeft)	{																																
				if (p.getX() == 0)
					return;
			}
			else {
				if (p.getX() == 12)
					return;
			}
		}
		parallelMoveHelper(ifLeft);																					//测试性移动
		for (Point p : Units) {
			if (p.getY() < 0)
				continue;
			if (b[p.getX()][p.getY()]) {																						//如果棋盘上该位置有单位，则移动非法，返回之前位置后后退出
				parallelMoveHelper(!ifLeft);
				return;
			}
		}
		//如果没有重合，则移动合法，不会返回之前的位置
	}
	
	/**
	 * 左右移动的辅助方法，只进行单纯移动
	 * @param ifLeft 向左还是向右
	 */
	private final void parallelMoveHelper(boolean ifLeft) {
		for (Point p: Units) {
			p.setX(p.getX() - (ifLeft ? 1 : -1));
		}
	}
	
	/**
	 * 骨牌旋转
	 * @throws GameException 旋转时发生的X坐标溢出的错误
	 */
	public abstract void rotate() throws GameException;
	
	/**
	 * 用于在刷新时，在随机位置，生成一个随机类型随机形状的新骨牌
	 * @return 新骨牌
	 * @throws GameException 生成新骨牌时发生了坐标溢出的错误
	 */
	public static final Tetrimino getRandomOne() throws GameException {
		int option = rand.nextInt(7)+1;
		switch(option) {
		case 1:
			return I_Tetrimino.getTetriminoOfI();
		case 2:
			return J_Tetrimino.getTetriminoOfJ();
		case 3:
			return L_Tetrimino.getTetriminoOfL();
		case 4:
			return O_Tetrimino.getTetriminoOfO();
		case 5:
			return Z_Tetrimino.getTetriminoOfZ();
		case 6:
			return T_Tetrimino.getTetriminoOfT();
		case 7:
			return S_Tetrimino.getTetriminoOfS();
		default:
			return I_Tetrimino.getTetriminoOfI();
		}
	}
	
	/**
	 * 使用下标获得骨牌的一个点
	 * @param index
	 * @return
	 */
	public final Point getPoint(int index) throws GameException{
		if (index < 0 || index > 3)
			throw new GameException("访问骨牌中的点的时候下标溢出，错误下标值：" + index);
		else return Units[index];
	}
	
	
	/**
	 * 深拷贝
	 */
	@Override
	public Tetrimino clone() throws CloneNotSupportedException {
		Tetrimino temp = (Tetrimino)super.clone();
		temp.Shape = Shape;
		temp.Units = Units.clone();
		return temp;
	}
	
	public void paint(Graphics2D g2, int w) {
		for (Point p : Units) {
			g2.setColor(new Color(40, 75, 220));
			g2.fillRect(p.getX()*w, p.getY()*w, w, w);
			g2.setColor(Color.BLACK);
			g2.drawRect(p.getX()*w, p.getY()*w, w, w);
		}
	}
	
	public void tipsPaint(Graphics2D g2, int w) {
		g2.setFont(g2.getFont().deriveFont(25.0F));
		g2.drawString("下一个骨牌：", 480, 50);
		for (int i = 0; i <= 3; i++) {
			g2.setColor(new Color(255, 0, 0));
			g2.fillRect(540+(Units[i].getX()-Units[0].getX())*w, 150+(Units[i].getY()-Units[1].getY())*w, w, w);
			g2.setColor(Color.BLACK);
			g2.drawRect(540+(Units[i].getX()-Units[0].getX())*w, 150+(Units[i].getY()-Units[1].getY())*w, w, w);
		}
	}
}

/**
 * I型骨牌的类
 * @author Asichurter
 *
 */
class I_Tetrimino extends Tetrimino{
	/**
	 * 用于产生随机数的对象
	 */
	private static Random rand = new Random();
	
	/**
	 * 用于在工厂方法中调用的，根据类型随机生成一个骨牌
	 * @param shape 骨牌形状
	 * @param units 骨牌的点
	 */
	private I_Tetrimino(int shape, Point[] units) {
		super(shape, units);
	}

	/**
	 * <p>用于外部调用，根据形状随机创建一个I型骨牌的方法。这个骨牌位于顶部</p>
	 * <p>创建的具体内部结构：[0, 1, 2, 3], 1为旋转中心</p>
	 * @return 随机生成的I型骨牌
	 * @throws GameException 生成骨牌时，产生点的X坐标错误
	 */
	public static I_Tetrimino getTetriminoOfI() throws GameException {
		int shape = rand.nextInt(2)+1;
		if (shape == 1) {
			Point firstPoint = new Point(rand.nextInt(10), -1);																																//第一个点随机生成，随后的点都跟随第一个点生成
			Point[] temp = {firstPoint, new Point(firstPoint.getX()+1, -1), new Point(firstPoint.getX()+2, -1), new Point(firstPoint.getX()+3, -1)};
			return new I_Tetrimino(shape, temp);
		}
		else{
			Point firstPoint = new Point(rand.nextInt(13), -1);
			Point[] temp = {firstPoint, new Point(firstPoint.getX(), -2), new Point(firstPoint.getX(), -3), new Point(firstPoint.getX(), -4)};
			return new I_Tetrimino(shape, temp);
		}
	}
	
	
	/**
	 * I型骨牌旋转，旋转中心：1
	 */
	@Override
	public void rotate() throws GameException {
		if (Shape == 1) {
			Units[0] = new Point(Units[1].getX(), Units[1].getY()-1);	
			Units[2] = new Point(Units[1].getX(), Units[1].getY()+1);
			Units[3] = new Point(Units[1].getX(), Units[1].getY()+2);
			Shape = 2;
		}
		else {
			if (Units[1].getX() <= 0 || Units[1].getX() >= 10)														//旋转后形状越界，非法
				return;
			else {
				Units[0] = new Point(Units[1].getX()-1, Units[1].getY());
				Units[2] = new Point(Units[1].getX()+1, Units[1].getY());
				Units[3] = new Point(Units[1].getX()+2, Units[1].getY());
				Shape = 1;
			}
		}
	}
	
	/**
	 * 覆盖克隆
	 */
	@Override
	public I_Tetrimino clone() throws CloneNotSupportedException {
		return (I_Tetrimino)super.clone();
	}
}

/**
 * J型骨牌
 * @author Asichurter
 *
 */
class J_Tetrimino extends Tetrimino{
	/**
	 * 用于产生随机数的对象
	 */
	private static Random rand = new Random();
	
	/**
	 * 用于在工厂方法中调用的，根据类型随机生成一个骨牌
	 * @param shape 骨牌形状
	 * @param units 骨牌的点
	 */
	private J_Tetrimino(int shape, Point[] units) {
		super(shape, units);
	}

	/**
	 * <p>用于外部调用，根据形状随机创建一个J型骨牌的方法。这个骨牌位于顶部</p>
	 * <p>创建的具体内部结构:</p>
	 * <p>. .             3</p>
	 * <p>.   .           2</p>
	 * <p>0     1</p>
	 * @return 随机生成的J型骨牌
	 * @throws GameException 生成骨牌时，产生点的X坐标错误
	 */
	public static J_Tetrimino getTetriminoOfJ() throws GameException {
		int shape = rand.nextInt(4)+1;																																								//随机生成一个初始类型
		if (shape == 1) {
			Point firstPoint = new Point(rand.nextInt(12), -1);																																//第一个点随机生成，随后的点都跟随第一个点生成
			Point[] temp = {firstPoint, new Point(firstPoint.getX()+1, -1), new Point(firstPoint.getX()+1, -2), new Point(firstPoint.getX()+1, -3)};
			return new J_Tetrimino(shape, temp);
		}
		else if (shape == 2) {
			Point firstPoint = new Point(rand.nextInt(11), -2);
			Point[] temp = {firstPoint, new Point(firstPoint.getX(), -1), new Point(firstPoint.getX()+1, -1), new Point(firstPoint.getX()+2, -1)};
			return new J_Tetrimino(shape, temp);
		}
		else if (shape == 3) {
			Point firstPoint = new Point(rand.nextInt(12)+1, -3);
			Point[] temp = {firstPoint, new Point(firstPoint.getX()-1, -3), new Point(firstPoint.getX()-1, -2), new Point(firstPoint.getX()-1, -1)};
			return new J_Tetrimino(shape, temp);
		}
		else{
			Point firstPoint = new Point(rand.nextInt(11)+2, -1);
			Point[] temp = {firstPoint, new Point(firstPoint.getX(), -2), new Point(firstPoint.getX()-1, -2), new Point(firstPoint.getX()-2, -2)};
			return new J_Tetrimino(shape, temp);
		}
	}
	
	
	/**
	 * J型骨牌旋转，旋转中心：1
	 */
	@Override
	public void rotate() throws GameException {
		if (Shape == 1 && Units[1].getX() <= 10) {																		//判断旋转后X坐标是否会因为越界而非法
			Units[0] = Units[2];
			Units[2] = new Point(Units[1].getX()+1, Units[1].getY());
			Units[3] = new Point(Units[1].getX()+2, Units[1].getY());
			Shape = 2;
		}
		else if (Shape == 2){
				Units[0] = Units[2];
				Units[2] = new Point(Units[1].getX(), Units[1].getY()+1);
				Units[3] = new Point(Units[1].getX(), Units[1].getY()+2);
				Shape = 3;
		}
		else if (Shape == 3 && Units[1].getX() >= 2){													//判断旋转后X坐标是否会因为越界而非法
			Units[0] = Units[2];
			Units[2] = new Point(Units[1].getX()-1, Units[1].getY());
			Units[3] = new Point(Units[1].getX()-2, Units[1].getY());
			Shape = 4;
		}
		else if (Shape == 4 && Units[1].getX() <= 11){													//判断旋转后X坐标是否会因为越界而非法
			Units[0] = Units[2];
			Units[2] = new Point(Units[1].getX(), Units[1].getY()-1);
			Units[3] = new Point(Units[1].getX(), Units[1].getY()-2);
			Shape = 1;
		}
	}
	
	/**
	 * 覆盖克隆
	 */
	@Override
	public J_Tetrimino clone() throws CloneNotSupportedException {
		return (J_Tetrimino)super.clone();
	}
}


/**
 * L型骨牌
 * @author Asichurter
 *
 */
class L_Tetrimino extends Tetrimino{
	/**
	 * 用于产生随机数的对象
	 */
	private static Random rand = new Random();
	
	/**
	 * 用于在工厂方法中调用的，根据类型随机生成一个骨牌
	 * @param shape 骨牌形状
	 * @param units 骨牌的点
	 */
	private L_Tetrimino(int shape, Point[] units) {
		super(shape, units);
	}

	/**
	 * <p>用于外部调用，根据形状随机创建一个L型骨牌的方法。这个骨牌位于顶部</p>
	 * <p>创建的具体内部结构:</p>
	 * <p>. .             3</p>
	 * <p>.   .           2</p>
	 * <p>.  .  1.  0</p>
	 * @return 随机生成的L型骨牌
	 * @throws GameException 生成骨牌时，产生点的X坐标错误
	 */
	public static L_Tetrimino getTetriminoOfL() throws GameException {
		int shape = rand.nextInt(4)+1;
		if (shape == 1) {
			Point firstPoint = new Point(rand.nextInt(12)+1, -1);																																//第一个点随机生成，随后的点都跟随第一个点生成
			Point[] temp = {firstPoint, new Point(firstPoint.getX()-1, -1), new Point(firstPoint.getX()-1, -2), new Point(firstPoint.getX()-1, -3)};
			return new L_Tetrimino(shape, temp);
		}
		else if (shape == 2) {
			Point firstPoint = new Point(rand.nextInt(11), -1);
			Point[] temp = {firstPoint, new Point(firstPoint.getX(), -2), new Point(firstPoint.getX()+1, -2), new Point(firstPoint.getX()+2, -2)};
			return new L_Tetrimino(shape, temp);
		}
		else if (shape == 3) {
			Point firstPoint = new Point(rand.nextInt(12), -3);
			Point[] temp = {firstPoint, new Point(firstPoint.getX()+1, -3), new Point(firstPoint.getX()+1, -2), new Point(firstPoint.getX()+1, -1)};
			return new L_Tetrimino(shape, temp);
		}
		else{
			Point firstPoint = new Point(rand.nextInt(11)+2, -2);
			Point[] temp = {firstPoint, new Point(firstPoint.getX(), -1), new Point(firstPoint.getX()-1, -1), new Point(firstPoint.getX()-2, -1)};
			return new L_Tetrimino(shape, temp);
		}
	}
	

	/**
	 * L型骨牌旋转，旋转中心：1
	 */	
	@Override
	public void rotate() throws GameException {
		if (Shape == 1 && Units[1].getX() <= 10) {																		//判断旋转后X坐标是否会因为越界而非法
			Units[2] = Units[0];
			Units[0] = new Point(Units[1].getX(), Units[1].getY()+1);
			Units[3] = new Point(Units[1].getX()+2, Units[1].getY());
			Shape = 2;
		}
		else if (Shape == 2){
				Units[2] = Units[0];
				Units[0] = new Point(Units[1].getX()-1, Units[1].getY());
				Units[3] = new Point(Units[1].getX(), Units[1].getY()+2);
				Shape = 3;
		}
		else if (Shape == 3 && Units[1].getX() >= 2){													//判断旋转后X坐标是否会因为越界而非法
			Units[2] = Units[0];
			Units[0] = new Point(Units[1].getX(), Units[1].getY()-1);
			Units[3] = new Point(Units[1].getX()-2, Units[1].getY());
			Shape = 4;
		}
		else if (Shape == 4 && Units[1].getX() <= 11){													//判断旋转后X坐标是否会因为越界而非法
			Units[2] = Units[0];
			Units[0] = new Point(Units[1].getX()+1, Units[1].getY());
			Units[3] = new Point(Units[1].getX(), Units[1].getY()-2);
			Shape = 1;
		}
	}
	
	/**
	 * 覆盖克隆
	 */
	@Override
	public L_Tetrimino clone() throws CloneNotSupportedException {
		return (L_Tetrimino)super.clone();
	}
}


/**
 * O型骨牌
 * @author Asichurter
 *
 */
class O_Tetrimino extends Tetrimino{

	/**
	 * 用于产生随机数的对象
	 */
	private static Random rand = new Random();
	
	/**
	 * 用于在工厂方法中调用的，根据类型随机生成一个骨牌
	 * @param units 骨牌的点
	 */
	private O_Tetrimino(Point[] units) {
		super(1, units);
	}

	/**
	 * <p>用于外部调用，根据形状随机创建一个O型骨牌的方法。这个骨牌位于顶部</p>
	 * <p>创建的具体内部结构:</p>
	 * <p>0      1</p>
	 * <p>3      2</p>
	 * @return 随机生成的O型骨牌
	 * @throws GameException 生成骨牌时，产生点的X坐标错误
	 */
	public static O_Tetrimino getTetriminoOfO() throws GameException {
		Point firstPoint = new Point(rand.nextInt(12), -2);
		Point[] temp = {firstPoint, new Point(firstPoint.getX()+1, -2), new Point(firstPoint.getX()+1, -1), new Point(firstPoint.getX(), -1)};
		return new O_Tetrimino(temp);
	}
		/**
	 * O型骨牌具有对称性，旋转无意义
	 */
	@Override
	public void rotate() throws GameException {}
	
	/**
	 * 覆盖克隆
	 */
	@Override
	public O_Tetrimino clone() throws CloneNotSupportedException {
		return (O_Tetrimino)super.clone();
	}
}

/**
 * 表征点的类
 * @author Asichurter
 *
 */
class Point {
	
	/**
	 * 点的X坐标
	 */
	private int X;
	/**
	 * 点的Y坐标
	 */
	private int Y;

	/**
	 * 使用坐标构建一个点
	 * @param x x坐标
	 * @param y y坐标
	 * @throws GameException 添加点时出现X坐标异常
	 */
	public Point(int x, int y) throws GameException {
		if (x < 0)
			throw new GameException("X坐标小于0");
		else if (x > 12)
			throw new GameException("X坐标大于12");
		this.X = x;
		this.Y = y;
	}
	
	/**
	 * 获得X坐标点值
	 */
	public int getX() {
		return this.X;
	}
	
	/**
	 * 获得Y坐标点值
	 */
	public int getY() {
		return this.Y;
	}
	
	/**
	 * 设置X坐标
	 */
	public void setX(int x) {
		this.X = x;
	}
	
	/**
	 * 设置Y坐标
	 */
	public void setY(int y) {
		this.Y = y;
	}

}



/**
 * S型骨牌的类
 * @author Asichurter
 *
 */
class S_Tetrimino extends Tetrimino{
	/**
	 * 用于产生随机数的对象
	 */
	private static Random rand = new Random();
	
	/**
	 * 用于在工厂方法中调用的，根据类型随机生成一个骨牌
	 * @param shape 骨牌形状
	 * @param units 骨牌的点
	 */
	private S_Tetrimino(int shape, Point[] units) {
		super(shape, units);
	}

	/**
	 * <p>用于外部调用，根据形状随机创建一个S型骨牌的方法。这个骨牌位于顶部</p>
	 * <p>创建的具体内部结构：</p>
	 * <p>....1      0</p>
	 * <p>3      2</p>
	 * @return 随机生成的S型骨牌
	 * @throws GameException 生成骨牌时，产生点的X坐标错误
	 */
	public static S_Tetrimino getTetriminoOfS() throws GameException {
		int shape = rand.nextInt(2)+1;																																																//随机生成shape，保证了shape的合法性
		if (shape == 1) {
			Point firstPoint = new Point(rand.nextInt(11)+2, -2);																																//第一个点随机生成，随后的点都跟随第一个点生成
			Point[] temp = {firstPoint, new Point(firstPoint.getX()-1, -2), new Point(firstPoint.getX()-1, -1), new Point(firstPoint.getX()-2, -1)};
			return new S_Tetrimino(shape, temp);
		}
		else{
			Point firstPoint = new Point(rand.nextInt(12)+1, -1);
			Point[] temp = {firstPoint, new Point(firstPoint.getX(), -2), new Point(firstPoint.getX()-1, -2), new Point(firstPoint.getX()-1, -3)};
			return new S_Tetrimino(shape, temp);
		}
	}
	
	
	/**
	 * S型骨牌旋转，旋转中心：1
	 */
	@Override
	public void rotate() throws GameException {
		if (Shape == 1) {
			Units[0] = Units[2];
			Units[0] = new Point(Units[1].getX()-1, Units[1].getY());
			Units[3] = new Point(Units[1].getX()-1, Units[1].getY()-1);
			Shape = 2;
		}
		else if(Units[1].getX() >= 1 && Units[1].getX() <= 11) {
				Units[0] = new Point(Units[1].getX()+1, Units[1].getY());
				Units[2] = new Point(Units[1].getX(), Units[1].getY()+1);
				Units[3] = new Point(Units[1].getX()-1, Units[1].getY()+1);
				Shape = 1;
		}
	}
	
	/**
	 * 覆盖克隆
	 */
	@Override
	public S_Tetrimino clone() throws CloneNotSupportedException {
		return (S_Tetrimino)super.clone();
	}
}



/**
 * T型骨牌
 * @author Asichurter
 *
 */
class T_Tetrimino extends Tetrimino{
	/**
	 * 用于产生随机数的对象
	 */
	private static Random rand = new Random();
	
	/**
	 * 用于在工厂方法中调用的，根据类型随机生成一个骨牌
	 * @param shape 骨牌形状
	 * @param units 骨牌的点
	 */
	private T_Tetrimino(int shape, Point[] units) {
		super(shape, units);
	}

	/**
	 * <p>用于外部调用，根据形状随机创建一个T型骨牌的方法。这个骨牌位于顶部</p>
	 * <p>创建的具体内部结构:</p>
	 * <p>.   .          3</p>
	 * <p>0.  1      2</p>
	 * @return 随机生成的T型骨牌
	 * @throws GameException 生成骨牌时，产生点的X坐标错误
	 */
	public static T_Tetrimino getTetriminoOfT() throws GameException {
		int shape = rand.nextInt(4)+1;
		if (shape == 1) {
			Point firstPoint = new Point(rand.nextInt(11), -1);																																//第一个点随机生成，随后的点都跟随第一个点生成
			Point[] temp = {firstPoint, new Point(firstPoint.getX()+1, -1), new Point(firstPoint.getX()+2, -1), new Point(firstPoint.getX()+1, -2)};
			return new T_Tetrimino(shape, temp);
		}
		else if (shape == 2) {
			Point firstPoint = new Point(rand.nextInt(12), -3);
			Point[] temp = {firstPoint, new Point(firstPoint.getX(), -2), new Point(firstPoint.getX(), -1), new Point(firstPoint.getX()+1, -2)};
			return new T_Tetrimino(shape, temp);
		}
		else if (shape == 3) {
			Point firstPoint = new Point(rand.nextInt(11)+2, -2);
			Point[] temp = {firstPoint, new Point(firstPoint.getX()-1, -2), new Point(firstPoint.getX()-2, -2), new Point(firstPoint.getX()-1, -1)};
			return new T_Tetrimino(shape, temp);
		}
		else{
			Point firstPoint = new Point(rand.nextInt(12)+1, -1);
			Point[] temp = {firstPoint, new Point(firstPoint.getX(), -2), new Point(firstPoint.getX(), -3), new Point(firstPoint.getX()-1, -2)};
			return new T_Tetrimino(shape, temp);
		}
	}
	

	/**
	 * T型骨牌旋转，旋转中心：1
	 */	
	@Override
	public void rotate() throws GameException {
		if (Shape == 1 ) {																		
			Units[0] = Units[3];
			Units[3] = Units[2];
			Units[2] = new Point(Units[1].getX(), Units[1].getY()+1);
			Shape = 2;
		}
		else if (Shape == 2 && Units[1].getX() >= 1 && Units[1].getX() <= 11){                       //判断旋转后X坐标是否会因为越界而非法
			Units[0] = Units[3];
			Units[3] = Units[2];
			Units[2] = new Point(Units[1].getX()-1, Units[1].getY());
			Shape = 3;
		}
		else if (Shape == 3){												
			Units[0] = Units[3];
			Units[3] = Units[2];
			Units[2] = new Point(Units[1].getX(), Units[1].getY()-1);
			Shape = 4;
		}
		else if (Shape == 4 && Units[1].getX() >= 1 && Units[1].getX() <= 11){					//判断旋转后X坐标是否会因为越界而非法
			Units[0] = Units[3];
			Units[3] = Units[2];
			Units[2] = new Point(Units[1].getX()+1, Units[1].getY());
			Shape = 1;
		}
	}
	
	/**
	 * 覆盖克隆
	 */
	@Override
	public T_Tetrimino clone() throws CloneNotSupportedException {
		return (T_Tetrimino)super.clone();
	}
}



/**
 * Z型骨牌的类
 * @author Asichurter
 *
 */
class Z_Tetrimino extends Tetrimino{
	/**
	 * 用于产生随机数的对象
	 */
	private static Random rand = new Random();
	
	/**
	 * 用于在工厂方法中调用的，根据类型随机生成一个骨牌
	 * @param shape 骨牌形状
	 * @param units 骨牌的点
	 */
	private Z_Tetrimino(int shape, Point[] units) {
		super(shape, units);
	}

	/**
	 * <p>用于外部调用，根据形状随机创建一个Z型骨牌的方法。这个骨牌位于顶部</p>
	 * <p>创建的具体内部结构：</p>
	 * <p>0      1</p>
	 * <p>....2      3</p>
	 * @return 随机生成的Z型骨牌
	 * @throws GameException 生成骨牌时，产生点的X坐标错误
	 */
	public static Z_Tetrimino getTetriminoOfZ() throws GameException {
		int shape = rand.nextInt(2)+1;
		if (shape == 1) {
			Point firstPoint = new Point(rand.nextInt(11), -2);																																//第一个点随机生成，随后的点都跟随第一个点生成
			Point[] temp = {firstPoint, new Point(firstPoint.getX()+1, -2), new Point(firstPoint.getX()+1, -1), new Point(firstPoint.getX()+2, -1)};
			return new Z_Tetrimino(shape, temp);
		}
		else{
			Point firstPoint = new Point(rand.nextInt(12)+1, -3);
			Point[] temp = {firstPoint, new Point(firstPoint.getX(), -2), new Point(firstPoint.getX()-1, -2), new Point(firstPoint.getX()-1, -1)};
			return new Z_Tetrimino(shape, temp);
		}
	}
	
	
	/**
	 * Z型骨牌旋转，旋转中心：1
	 */
	@Override
	public void rotate() throws GameException {
		if (Shape == 1) {
			Units[2] = Units[0];
			Units[0] = new Point(Units[1].getX(), Units[1].getY()-1);
			Units[3] = new Point(Units[1].getX()-1, Units[1].getY()+1);
			Shape = 2;
		}
		else if(Units[1].getX() >= 1 && Units[1].getX() <= 11) {
				Units[0] = new Point(Units[1].getX()-1, Units[1].getY());
				Units[2] = new Point(Units[1].getX(), Units[1].getY()+1);
				Units[3] = new Point(Units[1].getX()+1, Units[1].getY()+1);
				Shape = 1;
		}
	}
	
	/**
	 * 覆盖克隆
	 */
	@Override
	public Z_Tetrimino clone() throws CloneNotSupportedException {
		return (Z_Tetrimino)super.clone();
	}
}





