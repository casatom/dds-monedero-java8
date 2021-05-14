package dds.monedero.model;

import dds.monedero.exceptions.MaximaCantidadDepositosException;
import dds.monedero.exceptions.MaximoExtraccionDiarioException;
import dds.monedero.exceptions.MontoNegativoException;
import dds.monedero.exceptions.SaldoMenorException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Function;
import java.util.stream.Stream;

public class Cuenta {

  private double saldo;
  private List<Movimiento> movimientos = new ArrayList<>();

  public Cuenta() {
    saldo = 0;
  }

  //Esto no se usa nunca, pero mejor seria juntar con el otro constructor y poner null en el monto inicial si no vamos a poner nada
  //Monto inicial puede ser negativo
  public Cuenta(double montoInicial) {
    saldo = montoInicial;
  }

  //nunca se utiliza, y estos movimientos no van a tener impacto en el saldo de la cuenta
  //lo resolvi poniendo los movimientos e impactandolos en la cuenta
  public void setMovimientos(List<Movimiento> movimientos) {
    ListIterator<Movimiento> movimientoListIterator = movimientos.listIterator();
    while (movimientoListIterator.hasNext()){
      ponerOSacarPorMovimiento(movimientoListIterator.next());
    }
  }

  private void ponerOSacarPorMovimiento(Movimiento movimiento){
    if(movimiento.isDeposito()){
      ponerMovimientoDeposito(movimiento);
    }
    if(movimiento.isExtraccion()){
      sacarMovimientoExtraccion(movimiento);
    }
  }

  private void ponerMovimientoDeposito(Movimiento movimiento){
    validarMontoNegativo(movimiento.getMonto());
    movimiento.agregateA(this);
    
  }

  private void sacarMovimientoExtraccion(Movimiento movimiento){
    validarMontoNegativo(movimiento.getMonto());

    if (getSaldo() - movimiento.getMonto() < 0) {
      throw new SaldoMenorException("No puede sacar mas de " + getSaldo() + " $");
    }
    double montoExtraidoHoy = getMontoExtraidoA(movimiento.getFecha());
    double limite = 1000 - montoExtraidoHoy;
    if (movimiento.getMonto() > limite) {
      throw new MaximoExtraccionDiarioException("No puede extraer mas de $ " + 1000
          + " diarios, límite: " + limite);
    }
    
    movimiento.agregateA(this);
  }


  public void poner(double cuanto) {
    validarMontoNegativo(cuanto);
    //la funcion de deposito no estaba validando que sea de la misma fecha
    if (getMovimientos().stream().filter(movimiento -> movimiento.fueDepositado(LocalDate.now())).count() >= 3) {
      throw new MaximaCantidadDepositosException("Ya excedio los " + 3 + " depositos diarios");
    }

    new Movimiento(LocalDate.now(), cuanto, true).agregateA(this);
  }

  public void sacar(double cuanto) {
    validarMontoNegativo(cuanto);
    if (getSaldo() - cuanto < 0) {
      throw new SaldoMenorException("No puede sacar mas de " + getSaldo() + " $");
    }
    double montoExtraidoHoy = getMontoExtraidoA(LocalDate.now());
    double limite = 1000 - montoExtraidoHoy;
    if (cuanto > limite) {
      throw new MaximoExtraccionDiarioException("No puede extraer mas de $ " + 1000
          + " diarios, límite: " + limite);
    }
    new Movimiento(LocalDate.now(), cuanto, false).agregateA(this);
  }

  public void agregarMovimiento(LocalDate fecha, double cuanto, boolean esDeposito) {
    Movimiento movimiento = new Movimiento(fecha, cuanto, esDeposito);
    movimientos.add(movimiento);
  }

  public double getMontoExtraidoA(LocalDate fecha) {
    return getMovimientos().stream()
        .filter(movimiento -> !movimiento.isDeposito() && movimiento.getFecha().equals(fecha))
        .mapToDouble(Movimiento::getMonto)
        .sum();
  }

  public List<Movimiento> getMovimientos() {
    return movimientos;
  }

  public double getSaldo() {
    return saldo;
  }

  public void setSaldo(double saldo) {
    this.saldo = saldo;
  }
  
  private void validarMontoNegativo(double cuanto){
    if (cuanto <= 0) {
      throw new MontoNegativoException(cuanto + ": el monto a ingresar debe ser un valor positivo");
    }
  }

}
